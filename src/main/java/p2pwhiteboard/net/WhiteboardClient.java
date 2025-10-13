package p2pwhiteboard.net;

import java.net.*;

import p2pwhiteboard.model.DrawAction;
import p2pwhiteboard.model.Message;

import java.io.*;

public class WhiteboardClient {
    private ObjectOutputStream out;
    private Socket socket;

    public WhiteboardClient(String ip) throws IOException {
        this.socket = new Socket(ip, 5000);
        this.out = new ObjectOutputStream(socket.getOutputStream());
    }

    public WhiteboardClient(Socket accepted) throws IOException {
        this.socket = accepted;
        this.out = new ObjectOutputStream(socket.getOutputStream());
    }

    public void connect() throws IOException {
        // Connection is established in constructor
        // This method exists for compatibility
    }

    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void sendDrawAction(DrawAction action) throws IOException {
        out.writeObject(action);
        out.flush();
    }

    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
    }
}