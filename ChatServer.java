import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ChatServer — Multi-threaded TCP chat server.
 *
 * - Listens on PORT for incoming client connections.
 * - Spawns a new ClientHandler thread for every connected client.
 * - Maintains a thread-safe set of all active handlers.
 * - Broadcasts every message it receives to ALL connected clients.
 *
 * Built from EchoServer.java + ClientHandler.java concepts (JAVA_program folder).
 *
 * Compile:  javac *.java
 * Run:      java ChatServer
 */
public class ChatServer {

    static final int PORT = 5000;

    /**
     * Thread-safe set of all currently connected client handlers.
     * Synchronised externally via Collections.synchronizedSet.
     */
    private static final Set<ClientHandler> connectedClients =
            Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      Multi-Threaded Chat Server          ║");
        System.out.println("║      Listening on port " + PORT + "             ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Started. Waiting for clients...\n");

            // Accept loop — runs forever, one thread per client
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New connection from "
                        + clientSocket.getInetAddress().getHostAddress()
                        + ":" + clientSocket.getPort());

                ClientHandler handler = new ClientHandler(clientSocket, connectedClients);
                connectedClients.add(handler);

                Thread clientThread = new Thread(handler);
                clientThread.setDaemon(true);       // dies when main exits
                clientThread.start();

                System.out.println("[Server] Active clients: " + connectedClients.size());
            }

        } catch (IOException e) {
            System.err.println("[Server] Fatal error: " + e.getMessage());
        }
    }

    /**
     * Broadcast a message to every client EXCEPT the sender.
     * Called by ClientHandler on each received message.
     *
     * @param message  The formatted message to send
     * @param sender   The handler that sent the message (excluded from broadcast)
     */
    static void broadcast(String message, ClientHandler sender) {
        // Iterate over a snapshot to avoid ConcurrentModificationException
        synchronized (connectedClients) {
            for (ClientHandler client : connectedClients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Remove a client from the active set and notify others.
     */
    static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("[Server] Client removed. Active clients: " + connectedClients.size());
    }
}
