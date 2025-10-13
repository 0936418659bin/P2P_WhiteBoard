package p2pwhiteboard.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple UDP multicast discovery for peers on the same LAN and room.
 * Each node periodically announces (roomCode,userId,port) and listens for
 * others,
 * then attempts direct TCP connections to discovered peers.
 */
public class DiscoveryService {
    public interface PeerDiscoveredHandler {
        void onPeerDiscovered(String host, int port, String userId);
    }

    private static final String GROUP = "239.255.255.250";
    private static final int PORT = 4446;
    private static final int ANNOUNCE_INTERVAL_MS = 2000;

    private final String roomCode;
    private final String userId;
    private final int listenPort;
    private final PeerDiscoveredHandler handler;

    private Thread announceThread;
    private Thread listenThread;
    private volatile boolean running;

    // Keep a small cache to avoid repeated callbacks
    private final Set<String> seenPeers = new HashSet<>();

    public DiscoveryService(String roomCode, String userId, int listenPort, PeerDiscoveredHandler handler) {
        this.roomCode = roomCode;
        this.userId = userId;
        this.listenPort = listenPort;
        this.handler = handler;
    }

    public void start() {
        running = true;
        startAnnouncer();
        startListener();
    }

    public void stop() {
        running = false;
        if (announceThread != null) {
            try {
                announceThread.interrupt();
            } catch (Exception ignore) {
            }
        }
        if (listenThread != null) {
            try {
                listenThread.interrupt();
            } catch (Exception ignore) {
            }
        }
    }

    private void startAnnouncer() {
        announceThread = new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket()) {
                InetAddress group = InetAddress.getByName(GROUP);
                while (running) {
                    String payload = roomCode + "|" + userId + "|" + listenPort;
                    byte[] buf = payload.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress(group, PORT));
                    socket.send(packet);
                    try {
                        Thread.sleep(ANNOUNCE_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            } catch (IOException e) {
                // Best effort discovery; do not crash app on errors
                e.printStackTrace();
            }
        }, "DiscoveryAnnouncer");
        announceThread.setDaemon(true);
        announceThread.start();
    }

    private void startListener() {
        listenThread = new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket(PORT)) {
                InetAddress group = InetAddress.getByName(GROUP);
                socket.joinGroup(group);
                byte[] buf = new byte[512];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                            StandardCharsets.UTF_8);
                    String[] parts = msg.split("\\|");
                    if (parts.length != 3)
                        continue;
                    String r = parts[0];
                    String user = parts[1];
                    int port;
                    try {
                        port = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException nfe) {
                        continue;
                    }
                    if (!roomCode.equals(r))
                        continue; // different room
                    if (userId.equals(user))
                        continue; // self
                    String host = packet.getAddress().getHostAddress();
                    String key = host + ":" + port + "#" + user;
                    if (seenPeers.add(key) && handler != null) {
                        handler.onPeerDiscovered(host, port, user);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "DiscoveryListener");
        listenThread.setDaemon(true);
        listenThread.start();
    }
}
