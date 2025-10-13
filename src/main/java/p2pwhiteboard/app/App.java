package p2pwhiteboard.app;

import p2pwhiteboard.managers.RoomManager;
import p2pwhiteboard.managers.NetworkManager;
import p2pwhiteboard.ui.WhiteboardPanel;
import p2pwhiteboard.ui.components.ModernSidebar;
import p2pwhiteboard.ui.components.ModernToolbar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application class - simplified and modular
 */
public class App {
    private JFrame frame;
    private WhiteboardPanel whiteboardPanel;
    private ModernSidebar sidebar;
    private ModernToolbar toolbar;

    private RoomManager roomManager;
    private NetworkManager networkManager;

    public static void main(String[] args) {
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(() -> {
            App app = new App();
            app.start();
        });
    }

    private void start() {
        // Initialize managers
        roomManager = new RoomManager();
        networkManager = new NetworkManager();

        // Show room dialog
        RoomManager.RoomSession session = roomManager.createOrJoinRoom();
        if (session == null) {
            System.exit(0); // User cancelled
        }

        createUI();
        setupNetwork(session);
        setupEventHandlers();

        frame.setVisible(true);
    }

    private void createUI() {
        frame = new JFrame("P2P Whiteboard - Modern Edition");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        // Main layout
        frame.setLayout(new BorderLayout());

        // Create whiteboard panel
        whiteboardPanel = new WhiteboardPanel();
        whiteboardPanel.setBackground(Color.WHITE);

        // Create sidebar
        sidebar = new ModernSidebar(whiteboardPanel, this::handleHostLeave);

        // Create toolbar
        toolbar = new ModernToolbar(
                e -> whiteboardPanel.zoomIn(),
                e -> whiteboardPanel.zoomOut(),
                e -> whiteboardPanel.resetView());

        // Layout components
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(sidebar);
        mainSplit.setRightComponent(new JScrollPane(whiteboardPanel));
        mainSplit.setDividerLocation(300);
        mainSplit.setDividerSize(1);
        mainSplit.setBorder(null);

        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);

        // Update toolbar status
        updateStatus();
    }

    private void setupNetwork(RoomManager.RoomSession session) {
        // Start network session
        networkManager.startSession(
                session.roomCode,
                session.userName,
                session.userColor,
                session.isHost);

        // Set up network handlers
        networkManager.setDrawActionHandler(whiteboardPanel::addDrawAction);
        networkManager.setCursorHandler(whiteboardPanel::updatePeerCursor);
        // No host-leave semantics in pure P2P mesh

        // Set up whiteboard panel to broadcast actions
        whiteboardPanel.setDrawActionBroadcaster(networkManager::broadcastDrawAction);
        whiteboardPanel.setCursorBroadcaster(networkManager::broadcastCursor);
        whiteboardPanel.setClearBroadcaster(networkManager::broadcastClear);
    }

    private void setupEventHandlers() {
        // Window close handler
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (roomManager.isHost()) {
                    networkManager.broadcastHostLeave();
                }
                cleanup();
                System.exit(0);
            }
        });

        // Keyboard shortcuts
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = whiteboardPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = whiteboardPanel.getActionMap();

        // Undo/Redo
        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                whiteboardPanel.undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                whiteboardPanel.redo();
            }
        });

        // Zoom controls
        inputMap.put(KeyStroke.getKeyStroke("ctrl EQUALS"), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                whiteboardPanel.zoomIn();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl MINUS"), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                whiteboardPanel.zoomOut();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ctrl DIGIT0"), "resetView");
        actionMap.put("resetView", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                whiteboardPanel.resetView();
            }
        });
    }

    private void updateStatus() {
        toolbar.updateStatus(
                roomManager.getRoomCode(),
                roomManager.getUserName(),
                1 // TODO: Get actual peer count
        );
    }

    private void handleHostLeave(ActionEvent e) {
        cleanup();

        // Restart application
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
            start();
        });
    }

    private void handleHostLeaveFromServer() {
        cleanup();

        // Restart application
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
            start();
        });
    }

    private void cleanup() {
        if (networkManager != null) {
            networkManager.closeSession();
        }
        if (roomManager != null) {
            roomManager.reset();
        }
    }
}