# Java Multi-Threaded Chat Server

A terminal-based multi-user chat application built in pure Java — no external libraries.

Built from the networking and threading concepts in the `academics/JAVA_program/` folder:
- `EchoServer.java` + `ClientHandler.java` → evolved into a broadcast chat server
- `Multithreading.java` + `CounterRunnable.java` → separate sender/receiver threads in the client

## Architecture

```
ChatServer.java          — Main server: accepts connections, spawns threads, broadcasts
ClientHandler.java       — One thread per client: reads messages, broadcasts to others
ChatClient.java          — Terminal client: two threads (keyboard sender + server receiver)
```

### How it works

```
Client A ──┐
Client B ──┤── ChatServer (port 5000)
Client C ──┘
              │
              ├── Thread → ClientHandler(A)  reads A's messages → broadcasts to B, C
              ├── Thread → ClientHandler(B)  reads B's messages → broadcasts to A, C
              └── Thread → ClientHandler(C)  reads C's messages → broadcasts to A, B
```

Each `ClientHandler` runs on its own thread. The `ChatServer` maintains a `Collections.synchronizedSet` of all active handlers. Broadcasting iterates that set and calls `sendMessage()` (synchronized) on every handler except the sender.

The `ChatClient` uses **two threads**:
- Main thread reads keyboard input and sends to server
- `ReceiverTask` thread reads incoming server messages and prints them

This means typing doesn't block incoming messages.

## Prerequisites

- Java JDK 8 or higher
- Two terminal windows (one for server, one or more for clients)

## Compile

```bash
cd D:\Codes\projects\java-chat-server
javac *.java
```

## Run

### Terminal 1 — Start the server
```bash
java ChatServer
```
Output:
```
╔══════════════════════════════════════════╗
║      Multi-Threaded Chat Server          ║
║      Listening on port 5000             ║
╚══════════════════════════════════════════╝

[Server] Started. Waiting for clients...
```

### Terminal 2 — Connect as first client
```bash
java ChatClient
```

### Terminal 3 — Connect as second client
```bash
java ChatClient
```

### Connect to a remote server
```bash
java ChatClient 192.168.1.10          # custom host, default port 5000
java ChatClient 192.168.1.10 6000     # custom host and port
```

## Chat Commands

| Command | Description |
|---------|-------------|
| (any text) | Broadcast message to all other users |
| `/list` | Show all currently connected users |
| `/quit` | Disconnect gracefully |

## Sample Session

```
Welcome to the Chat Server!
Enter your username: Dhruv
──────────────────────────────────────────────
Joined as: Dhruv
Commands: /list — show active users | /quit — disconnect
──────────────────────────────────────────────
[+] Raj joined the chat.
> hello everyone
[You → All] hello everyone
[Raj] hey Dhruv!
> /list
Active users (2):
  • Dhruv (you)
  • Raj
> /quit
Goodbye, Dhruv!

Disconnected. Goodbye!
```

## Key Java Concepts Used

| Concept | Where |
|---------|-------|
| `ServerSocket` + `Socket` | `ChatServer.java` — TCP connection setup |
| `Runnable` + `Thread` | `ClientHandler.java`, `ChatClient.ReceiverTask` |
| `synchronized` | `ClientHandler.sendMessage()` — thread-safe writes |
| `Collections.synchronizedSet` | `ChatServer` — thread-safe client registry |
| `BufferedReader` / `PrintWriter` | All files — stream I/O over sockets |
| `volatile boolean` | `ChatClient.running` — safe cross-thread flag |
| `Thread.setDaemon(true)` | Receiver thread — auto-terminates on main exit |

## Files

```
java-chat-server/
├── ChatServer.java      — Server entry point
├── ClientHandler.java   — Per-client thread handler
├── ChatClient.java      — Client entry point (two-thread design)
└── README.md
```
