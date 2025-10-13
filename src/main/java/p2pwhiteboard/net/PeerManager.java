package p2pwhiteboard.net;

import java.util.ArrayList;
import java.util.List;
import p2pwhiteboard.model.DrawAction;
import p2pwhiteboard.model.Message;

public class PeerManager {
    private List<WhiteboardClient> peers = new ArrayList<>();

    // Handler interfaces
    public interface DrawActionHandler {
        void onDrawAction(p2pwhiteboard.model.DrawAction action);
    }

    public interface CursorHandler {
        void onCursorUpdate(String userId, java.awt.Color userColor, int x, int y);
    }

    private DrawActionHandler drawActionHandler;
    private CursorHandler cursorHandler;

    // Handle incoming messages from any source (server listener or other)
    public void handleIncoming(Message m) {
        if (m == null)
            return;
        switch (m.type) {
            case DRAW:
                if (drawActionHandler != null && m.draw != null)
                    drawActionHandler.onDrawAction(m.draw);
                break;
            case CLEAR:
                if (drawActionHandler != null)
                    drawActionHandler.onDrawAction(
                            new p2pwhiteboard.model.DrawAction(p2pwhiteboard.model.DrawAction.ToolType.CLEAR, 0, 0, 0,
                                    0,
                                    java.awt.Color.WHITE, 1, false));
                break;
            case CURSOR:
                if (cursorHandler != null)
                    cursorHandler.onCursorUpdate(m.userId, m.userColor, m.cursorX, m.cursorY);
                break;
            case JOIN:
            case PING:
            case HOST_LEAVE:
                // Ignored in pure P2P mesh
                break;
        }
    }

    public void addPeer(WhiteboardClient client) {
        peers.add(client);
    }

    public void removePeer(WhiteboardClient client) {
        peers.remove(client);
    }

    public List<WhiteboardClient> getPeers() {
        return peers;
    }

    public void broadcastDrawAction(DrawAction action) {
        // Notify local handler first
        if (drawActionHandler != null) {
            drawActionHandler.onDrawAction(action);
        }

        // Broadcast to peers
        for (WhiteboardClient client : peers) {
            try {
                client.sendMessage(Message.draw(currentRoom, currentUserId, currentUserColor, action));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String currentRoom;
    private String currentUserId;
    private java.awt.Color currentUserColor;

    public void setSession(String room, String userId, java.awt.Color color) {
        this.currentRoom = room;
        this.currentUserId = userId;
        this.currentUserColor = color;
    }

    public void broadcastCursor(int x, int y) {
        // Notify local handler first
        if (cursorHandler != null) {
            cursorHandler.onCursorUpdate(currentUserId, currentUserColor, x, y);
        }

        // Broadcast to peers
        for (WhiteboardClient client : peers) {
            try {
                client.sendMessage(Message.cursor(currentRoom, currentUserId, currentUserColor, x, y));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastClear() {
        for (WhiteboardClient client : peers) {
            try {
                client.sendMessage(Message.clear(currentRoom, currentUserId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastHostLeave() {
        for (WhiteboardClient client : peers) {
            try {
                client.sendMessage(Message.hostLeave(currentRoom, currentUserId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setDrawActionHandler(DrawActionHandler handler) {
        this.drawActionHandler = handler;
    }

    public void setCursorHandler(CursorHandler handler) {
        this.cursorHandler = handler;
    }
}
