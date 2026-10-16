import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

/**
 * ClientHandler — Runnable that manages one connected chat client.
 *
 * Each ClientHandler runs on its own thread (spawned by ChatServer).
 * It:
 *   1. Reads the client's chosen username on first connect
 *   2. Announces the join to all other clients
 *   3. Continuously reads incoming messages and broadcasts them
 *   4. Cleans up and announces departure when the client disconnects
 *
 * Based on ClientHandler.java from JAVA_program — extended with:
 *   - Username negotiation
 *   - Broadcast to all clients instead of echo back
 *   - /quit command support
 *   - /list command to see active users
 *   - Thread-safe access to the shared client set
 */
public class ClientHandler implements Runnable {

    private final Socket       socket;
    private final Set<ClientHandler> allClients;

    private BufferedReader reader;
    private PrintWriter    writer;
    private String         username;

    public ClientHandler(Socket socket, Set<ClientHandler> allClients) {
        this.socket     = socket;
        this.allClients = allClients;
    }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress()
                          + ":" + socket.getPort();
        try {
            // ── Set up I/O streams ────────────────────────────────
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // ── Username negotiation ──────────────────────────────
            writer.println("Welcome to the Chat Server!");
            writer.println("Enter your username: ");

            username = reader.readLine();
            if (username == null || username.isBlank()) {
                username = "User_" + socket.getPort();
            }
            username = username.trim().replaceAll("\\s+", "_");

            writer.println("──────────────────────────────────────────────");
            writer.println("Joined as: " + username);
            writer.println("Commands: /list — show active users | /quit — disconnect");
            writer.println("──────────────────────────────────────────────");

            System.out.println("[Server] " + username + " joined (" + clientAddr + ")");

            // Notify all OTHER clients
            ChatServer.broadcast("[+] " + username + " joined the chat.", this);

            // ── Main message loop ─────────────────────────────────
            String incoming;
            while ((incoming = reader.readLine()) != null) {
                incoming = incoming.trim();
                if (incoming.isEmpty()) continue;

                // ── /quit command ─────────────────────────────────
                if (incoming.equalsIgnoreCase("/quit")) {
                    writer.println("Goodbye, " + username + "!");
                    break;
                }

                // ── /list command ─────────────────────────────────
                if (incoming.equalsIgnoreCase("/list")) {
                    StringBuilder list = new StringBuilder("Active users (" + allClients.size() + "):\n");
                    synchronized (allClients) {
                        for (ClientHandler c : allClients) {
                            list.append("  • ").append(c.username);
                            if (c == this) list.append(" (you)");
                            list.append("\n");
                        }
                    }
                    writer.println(list.toString().trim());
                    continue;
                }

                // ── Regular message — broadcast ───────────────────
                String formatted = "[" + username + "] " + incoming;
                System.out.println(formatted);
                ChatServer.broadcast(formatted, this);

                // Echo back to sender with timestamp
                writer.println("[You → All] " + incoming);
            }

        } catch (IOException e) {
            System.out.println("[Server] Connection lost with " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Send a message to this client's output stream.
     * Called by ChatServer.broadcast() from other handler threads — must be thread-safe.
     */
    public synchronized void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    /**
     * Clean up: close streams, remove from active set, notify others.
     */
    private void disconnect() {
        ChatServer.removeClient(this);
        if (username != null) {
            ChatServer.broadcast("[-] " + username + " left the chat.", this);
            System.out.println("[Server] " + username + " disconnected.");
        }
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[Server] Error closing socket: " + e.getMessage());
        }
    }

    /** Used for display/logging. */
    public String getUsername() {
        return username;
    }
}
