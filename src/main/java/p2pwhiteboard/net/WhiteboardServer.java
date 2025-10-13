package p2pwhiteboard.net;

import java.net.*;

import p2pwhiteboard.model.DrawAction;
import p2pwhiteboard.model.Message;

import java.io.*;

/**
 * Symmetric peer listener: accepts inbound TCP connections and forwards
 * incoming messages to the shared PeerManager.
 */
public class WhiteboardServer extends Thread {
    private ServerSocket serverSocket;
    private final PeerManager peerManager;

    public WhiteboardServer(PeerManager peerManager) {
        this.peerManager = peerManager;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(5000);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                // Wrap accepted socket as a client for outbound messages and track it
                try {
                    WhiteboardClient client = new WhiteboardClient(socket);
                    peerManager.addPeer(client);
                } catch (IOException ioe) {
                    // If we cannot create an outbound stream, still try to consume inbound
                }
                // Start a reader thread per connection
                Thread t = new Thread(() -> handleConnection(socket), "WB-InboundReader");
                t.setDaemon(true);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    Message m = (Message) obj;
                    peerManager.handleIncoming(m);
                } else if (obj instanceof DrawAction) {
                    // backward compatibility with older clients
                    DrawAction da = (DrawAction) obj;
                    peerManager.handleIncoming(Message.draw(null, null, null, da));
                }
            }
        } catch (Exception e) {
            // Connection closed or error; ignore
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }

    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException ignore) {
        }
        try {
            interrupt();
        } catch (Exception ignore) {
        }
    }

    public void close() throws IOException {
        shutdown();
    }
}