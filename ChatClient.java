import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ConnectException;
import java.util.Scanner;

/**
 * ChatClient — Terminal chat client.
 *
 * Connects to ChatServer on HOST:PORT and runs two threads:
 *   1. ReceiverThread — continuously reads and prints incoming messages
 *   2. Main thread    — reads user keyboard input and sends it to server
 *
 * Using two separate threads means the user can type while messages arrive —
 * no blocking. This is the key improvement over the original EchoClient.java
 * which used a single thread and would block on readLine().
 *
 * Usage:
 *   java ChatClient                      — connects to localhost:5000
 *   java ChatClient 192.168.1.10         — connects to given host
 *   java ChatClient 192.168.1.10 6000    — connects to given host:port
 */
public class ChatClient {

    private static volatile boolean running = true;

    public static void main(String[] args) {
        // Parse optional host/port arguments
        String host = args.length > 0 ? args[0] : "localhost";
        int    port = args.length > 1 ? Integer.parseInt(args[1]) : ChatServer.PORT;

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         Multi-Threaded Chat Client        ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("Connecting to " + host + ":" + port + " ...");

        try (
            Socket socket = new Socket(host, port);
            BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter serverWriter = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            Scanner keyboard = new Scanner(System.in)
        ) {
            System.out.println("Connected!\n");

            // ── Thread 1: Receive messages from server ────────────────
            Thread receiverThread = new Thread(new ReceiverTask(serverReader));
            receiverThread.setDaemon(true);
            receiverThread.start();

            // ── Main thread: Send messages from keyboard ──────────────
            while (running && keyboard.hasNextLine()) {
                String input = keyboard.nextLine().trim();
                if (input.isEmpty()) continue;

                serverWriter.println(input);

                // If user typed /quit, stop reading after server acknowledges
                if (input.equalsIgnoreCase("/quit")) {
                    running = false;
                    Thread.sleep(300);   // small wait for goodbye message
                    break;
                }
            }

        } catch (ConnectException e) {
            System.err.println("\n[Error] Could not connect to " + host + ":" + port);
            System.err.println("Make sure ChatServer is running first.");
        } catch (IOException e) {
            if (running) {
                System.err.println("\n[Error] Connection lost: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\nDisconnected. Goodbye!");
    }

    // ── Inner class: Receiver thread task ────────────────────────────────────

    /**
     * ReceiverTask — runs on a separate thread, continuously reads
     * messages arriving from the server and prints them to the console.
     *
     * Demonstrates the Runnable interface pattern from CounterRunnable.java
     * and the threading concepts from Multithreading.java.
     */
    private static class ReceiverTask implements Runnable {

        private final BufferedReader reader;

        ReceiverTask(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    // Print message on its own line, then re-print the prompt
                    System.out.println("\r" + line);
                    System.out.print("> ");
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("\n[Receiver] Connection closed by server.");
                }
            } finally {
                running = false;
            }
        }
    }
}
