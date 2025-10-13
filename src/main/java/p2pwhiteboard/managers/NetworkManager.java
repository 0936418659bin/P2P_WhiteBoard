package p2pwhiteboard.managers;

import p2pwhiteboard.model.DrawAction;
import p2pwhiteboard.net.PeerManager;
import p2pwhiteboard.net.WhiteboardServer;
import p2pwhiteboard.net.WhiteboardClient;
import p2pwhiteboard.net.DiscoveryService;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Manages network operations and peer connections
 */
public class NetworkManager {
    private PeerManager peerManager;
    private WhiteboardServer server;
    private DiscoveryService discovery;

    private String currentRoomCode;
    private String currentUserId;
    private Color currentUserColor;

    public void startSession(String roomCode, String userId, Color userColor, boolean isHost) {
        this.currentRoomCode = roomCode;
        this.currentUserId = userId;
        this.currentUserColor = userColor;

        try {
            // Initialize peer manager first
            peerManager = new PeerManager();
            peerManager.setSession(roomCode, userId, userColor);

            // Start symmetric server listener on TCP 5000
            server = new WhiteboardServer(peerManager);
            server.start();
            System.out.println("Peer listener started on 5000");

            // Start discovery and connect to discovered peers directly
            discovery = new DiscoveryService(roomCode, userId, 5000, (host, port, discoveredUser) -> {
                try {
                    WhiteboardClient c = new WhiteboardClient(host);
                    c.connect();
                    peerManager.addPeer(c);
                    System.out.println("Connected to peer " + discoveredUser + " at " + host + ":" + port);
                } catch (IOException ioe) {
                    System.err.println("Failed to connect to peer at " + host + ":" + port + " - " + ioe.getMessage());
                }
            });
            discovery.start();

        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcastDrawAction(DrawAction action) {
        if (peerManager != null) {
            peerManager.broadcastDrawAction(action);
        }
    }

    public void broadcastCursor(int x, int y) {
        if (peerManager != null) {
            peerManager.broadcastCursor(x, y);
        }
    }

    public void broadcastClear() {
        if (peerManager != null) {
            peerManager.broadcastClear();
        }
    }

    public void broadcastHostLeave() {
        // No-op in pure P2P mesh
    }

    public void setDrawActionHandler(PeerManager.DrawActionHandler handler) {
        if (peerManager != null) {
            peerManager.setDrawActionHandler(handler);
        }
    }

    public void setCursorHandler(PeerManager.CursorHandler handler) {
        if (peerManager != null) {
            peerManager.setCursorHandler(handler);
        }
    }

    public void setHostLeaveHandler(WhiteboardServer.HostLeaveHandler handler) {
        // No-op in pure P2P mesh
    }

    public void closeSession() {
        try {
            if (server != null) {
                server.close();
                server = null;
            }
            if (discovery != null) {
                discovery.stop();
                discovery = null;
            }
            if (peerManager != null) {
                peerManager = null;
            }
        } catch (IOException e) {
            System.err.println("Error closing network session: " + e.getMessage());
        }
    }

    private String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public String getCurrentRoomCode() {
        return currentRoomCode;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public Color getCurrentUserColor() {
        return currentUserColor;
    }
}
