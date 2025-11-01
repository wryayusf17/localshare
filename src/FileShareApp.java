import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main application class for LocalShare - A LAN File Sharing Application
 * Provides GUI interface with Send and Receive functionality
 */
public class FileShareApp extends JFrame {
    // Constants
    private static final String APP_TITLE = "LocalShare";
    private static final int WINDOW_WIDTH = 700;
    private static final int WINDOW_HEIGHT = 550;
    
    // Color scheme - Modern and friendly
    private static final Color PRIMARY_COLOR = new Color(66, 133, 244);      // Blue
    private static final Color PRIMARY_DARK = new Color(51, 103, 214);
    private static final Color SECONDARY_COLOR = new Color(52, 168, 83);     // Green
    private static final Color SECONDARY_DARK = new Color(42, 138, 73);
    private static final Color BACKGROUND_COLOR = new Color(245, 248, 250);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(60, 64, 67);
    private static final Color TEXT_SECONDARY = new Color(128, 134, 139);
    private static final Color BORDER_COLOR = new Color(218, 220, 224);
    
    // GUI Components
    private JButton sendButton;
    private JButton receiveButton;
    private JTextArea statusLog;
    private JScrollPane scrollPane;
    private JLabel statusLabel;
    private JPanel dropZonePanel;
    private JLabel dropZoneLabel;
    private File selectedFile = null;
    
    // Core components
    private BroadcastService broadcastService;
    private FileReceiver fileReceiver;
    private FileSender fileSender;
    
    // Application state
    private boolean isReceiving = false;
    private ConcurrentHashMap<String, ReceiverInfo> discoveredReceivers;
    
    public FileShareApp() {
        discoveredReceivers = new ConcurrentHashMap<>();
        initializeGUI();
        initializeServices();
    }
    
    /**
     * Initialize the graphical user interface
     */
    private void initializeGUI() {
        setTitle(APP_TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Create main content area
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        // Create footer with status
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
        
        // Center window on screen
        setLocationRelativeTo(null);
        
        logStatus("Ready to share files on your local network");
    }
    
    /**
     * Create the header panel with title and branding
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // App title
        JLabel titleLabel = new JLabel("📁 LocalShare");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Share files instantly over your local network");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        
        header.add(titlePanel, BorderLayout.WEST);
        
        return header;
    }
    
    /**
     * Create the main panel with action buttons and drop zone
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Create action cards panel
        JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        actionsPanel.setOpaque(false);
        
        // Send card
        JPanel sendCard = createSendCard();
        actionsPanel.add(sendCard);
        
        // Receive card
        JPanel receiveCard = createReceiveCard();
        actionsPanel.add(receiveCard);
        
        mainPanel.add(actionsPanel, BorderLayout.NORTH);
        
        // Create status log panel
        JPanel logPanel = createLogPanel();
        mainPanel.add(logPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Create the send file card with drag-and-drop
     */
    private JPanel createSendCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Icon and title
        JLabel iconLabel = new JLabel("📤");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel titleLabel = new JLabel("Send Files");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel descLabel = new JLabel("Click or drag files here");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_SECONDARY);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Create drop zone
        dropZonePanel = new JPanel(new BorderLayout());
        dropZonePanel.setBackground(new Color(245, 248, 250));
        dropZonePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(PRIMARY_COLOR, 2, 5, 5, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dropZonePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        dropZoneLabel = new JLabel("Drop file here or click to browse");
        dropZoneLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dropZoneLabel.setForeground(TEXT_SECONDARY);
        dropZoneLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dropZonePanel.add(dropZoneLabel, BorderLayout.CENTER);
        
        // Add drag-and-drop support
        enableDragAndDrop(dropZonePanel);
        
        // Add click to browse
        dropZonePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                browseForFile();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                dropZonePanel.setBackground(new Color(240, 243, 245));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                dropZonePanel.setBackground(new Color(245, 248, 250));
            }
        });
        
        // Send button
        sendButton = createStyledButton("Send File", PRIMARY_COLOR, PRIMARY_DARK);
        sendButton.addActionListener(e -> handleSendFile());
        
        // Layout
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        topPanel.setOpaque(false);
        topPanel.add(iconLabel);
        topPanel.add(titleLabel);
        topPanel.add(descLabel);
        
        card.add(topPanel, BorderLayout.NORTH);
        card.add(dropZonePanel, BorderLayout.CENTER);
        card.add(sendButton, BorderLayout.SOUTH);
        
        return card;
    }
    
    /**
     * Create the receive card
     */
    private JPanel createReceiveCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Icon and title
        JLabel iconLabel = new JLabel("📥");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel titleLabel = new JLabel("Receive Files");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel descLabel = new JLabel("Listen for incoming files");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_SECONDARY);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Status indicator
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setOpaque(false);
        
        JLabel statusDot = new JLabel("●");
        statusDot.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        statusDot.setForeground(TEXT_SECONDARY);
        
        statusLabel = new JLabel("Not receiving");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(TEXT_SECONDARY);
        
        statusPanel.add(statusDot);
        statusPanel.add(statusLabel);
        
        // Receive button
        receiveButton = createStyledButton("Start Receiving", SECONDARY_COLOR, SECONDARY_DARK);
        receiveButton.addActionListener(e -> toggleReceiving());
        
        // Layout
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        topPanel.setOpaque(false);
        topPanel.add(iconLabel);
        topPanel.add(titleLabel);
        topPanel.add(descLabel);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));
        centerPanel.add(statusPanel, BorderLayout.CENTER);
        
        card.add(topPanel, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(receiveButton, BorderLayout.SOUTH);
        
        return card;
    }
    
    /**
     * Create the log panel
     */
    private JPanel createLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout(0, 10));
        logPanel.setOpaque(false);
        
        JLabel logTitle = new JLabel("Activity Log");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logTitle.setForeground(TEXT_COLOR);
        
        statusLog = new JTextArea();
        statusLog.setEditable(false);
        statusLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        statusLog.setLineWrap(true);
        statusLog.setWrapStyleWord(true);
        statusLog.setBackground(CARD_COLOR);
        statusLog.setForeground(TEXT_COLOR);
        statusLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        scrollPane = new JScrollPane(statusLog);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setPreferredSize(new Dimension(0, 150));
        
        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        return logPanel;
    }
    
    /**
     * Create the footer panel
     */
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD_COLOR);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 25, 10, 25)
        ));
        
        JLabel infoLabel = new JLabel("Files saved to: ReceivedFiles/");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLabel.setForeground(TEXT_SECONDARY);
        
        footer.add(infoLabel, BorderLayout.WEST);
        
        return footer;
    }
    
    /**
     * Create a styled button
     */
    private JButton createStyledButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 40));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    /**
     * Enable drag-and-drop on a component
     */
    private void enableDragAndDrop(JPanel panel) {
        panel.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                    
                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        if (file.isFile()) {
                            selectFile(file);
                        } else {
                            JOptionPane.showMessageDialog(FileShareApp.this,
                                "Please drop a file, not a folder.",
                                "Invalid Selection",
                                JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    logStatus("ERROR: Drag-and-drop failed - " + ex.getMessage());
                }
            }
            
            @Override
            public synchronized void dragEnter(DropTargetDragEvent evt) {
                dropZonePanel.setBackground(new Color(230, 240, 255));
                dropZonePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createDashedBorder(PRIMARY_DARK, 3, 5, 5, true),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }
            
            @Override
            public synchronized void dragExit(DropTargetEvent evt) {
                dropZonePanel.setBackground(new Color(245, 248, 250));
                dropZonePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createDashedBorder(PRIMARY_COLOR, 2, 5, 5, true),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }
        });
    }
    
    /**
     * Browse for file using file chooser
     */
    private void browseForFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Send");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectFile(fileChooser.getSelectedFile());
        }
    }
    
    /**
     * Select a file for sending
     */
    private void selectFile(File file) {
        selectedFile = file;
        String fileName = file.getName();
        long fileSize = file.length();
        String sizeStr = formatFileSize(fileSize);
        
        dropZoneLabel.setText("✓ " + fileName + " (" + sizeStr + ")");
        dropZoneLabel.setForeground(SECONDARY_COLOR);
        dropZonePanel.setBackground(new Color(232, 245, 233));
        
        logStatus("Selected: " + fileName + " (" + sizeStr + ")");
    }
    
    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Initialize network services
     */
    private void initializeServices() {
        broadcastService = new BroadcastService(this);
        fileReceiver = new FileReceiver(this);
        fileSender = new FileSender(this);
    }
    
    /**
     * Handle Send File button click
     */
    private void handleSendFile() {
        if (isReceiving) {
            JOptionPane.showMessageDialog(this,
                "Cannot send files while in receiving mode.\nPlease stop receiving first.",
                "Cannot Send",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if file is selected
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a file first by dragging it to the drop zone\nor clicking the drop zone to browse.",
                "No File Selected",
                JOptionPane.INFORMATION_MESSAGE);
            browseForFile();
            return;
        }
        
        if (!selectedFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "The selected file no longer exists.",
                "File Not Found",
                JOptionPane.ERROR_MESSAGE);
            selectedFile = null;
            dropZoneLabel.setText("Drop file here or click to browse");
            dropZoneLabel.setForeground(TEXT_SECONDARY);
            dropZonePanel.setBackground(new Color(245, 248, 250));
            return;
        }
        
        logStatus("🔍 Searching for receivers on the network...");
        
        // Start discovery in background thread
        new Thread(() -> {
            try {
                broadcastService.discoverReceivers(3000); // 3 second discovery timeout
                
                SwingUtilities.invokeLater(() -> {
                    if (discoveredReceivers.isEmpty()) {
                        logStatus("❌ No receivers found on the network");
                        JOptionPane.showMessageDialog(this,
                            "<html><body style='width: 300px'>" +
                            "<h3>No Receivers Found</h3>" +
                            "<p>Make sure another device has clicked <b>'Start Receiving'</b>.</p>" +
                            "<p>Both devices must be on the same network.</p>" +
                            "</body></html>",
                            "No Receivers Found",
                            JOptionPane.WARNING_MESSAGE);
                    } else {
                        showReceiverSelectionDialog();
                    }
                });
            } catch (Exception e) {
                logStatus("ERROR during discovery: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Show dialog to select a receiver from discovered devices
     */
    private void showReceiverSelectionDialog() {
        // Create custom dialog
        JDialog dialog = new JDialog(this, "Select Receiver", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(BACKGROUND_COLOR);
        
        JLabel titleLabel = new JLabel("Select a device to send to:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_COLOR);
        
        // Create list of receivers
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (ReceiverInfo receiver : discoveredReceivers.values()) {
            listModel.addElement("📱 " + receiver.getIp() + ":" + receiver.getPort());
        }
        
        JList<String> receiverList = new JList<>(listModel);
        receiverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        receiverList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        receiverList.setBackground(CARD_COLOR);
        receiverList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        receiverList.setSelectedIndex(0);
        
        JScrollPane listScroll = new JScrollPane(receiverList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JButton sendButton = createStyledButton("Send File", PRIMARY_COLOR, PRIMARY_DARK);
        sendButton.setPreferredSize(new Dimension(120, 35));
        sendButton.addActionListener(e -> {
            int selectedIndex = receiverList.getSelectedIndex();
            if (selectedIndex >= 0) {
                String selected = listModel.get(selectedIndex);
                // Extract IP:PORT from "📱 IP:PORT"
                String ipPort = selected.substring(2); // Remove emoji
                ReceiverInfo receiver = discoveredReceivers.get(ipPort);
                if (receiver != null && selectedFile != null) {
                    dialog.dispose();
                    fileSender.sendFile(selectedFile, receiver);
                    // Reset file selection after sending
                    selectedFile = null;
                    dropZoneLabel.setText("Drop file here or click to browse");
                    dropZoneLabel.setForeground(TEXT_SECONDARY);
                    dropZonePanel.setBackground(new Color(245, 248, 250));
                }
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(sendButton);
        
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(listScroll, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(contentPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Toggle receiving mode on/off
     */
    private void toggleReceiving() {
        if (!isReceiving) {
            // Start receiving
            try {
                fileReceiver.startReceiving();
                broadcastService.startBroadcasting();
                isReceiving = true;
                receiveButton.setText("Stop Receiving");
                receiveButton.setBackground(new Color(234, 67, 53)); // Red
                sendButton.setEnabled(false);
                statusLabel.setText("Receiving...");
                statusLabel.setForeground(SECONDARY_COLOR);
                logStatus("✅ Receiving mode started - Broadcasting presence on network");
            } catch (Exception e) {
                logStatus("ERROR: Failed to start receiving - " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "<html><body style='width: 300px'>" +
                    "<h3>Failed to Start Receiving</h3>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body></html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Stop receiving
            broadcastService.stopBroadcasting();
            fileReceiver.stopReceiving();
            isReceiving = false;
            receiveButton.setText("Start Receiving");
            receiveButton.setBackground(SECONDARY_COLOR);
            sendButton.setEnabled(true);
            statusLabel.setText("Not receiving");
            statusLabel.setForeground(TEXT_SECONDARY);
            logStatus("⏹ Receiving mode stopped");
        }
    }
    
    /**
     * Log status message to the status log area
     */
    public void logStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            statusLog.append("[" + timestamp + "] " + message + "\n");
            statusLog.setCaretPosition(statusLog.getDocument().getLength());
        });
    }
    
    /**
     * Add a discovered receiver to the list
     */
    public void addDiscoveredReceiver(String ip, int port) {
        String key = ip + ":" + port;
        discoveredReceivers.put(key, new ReceiverInfo(ip, port));
        logStatus("Discovered receiver at " + key);
    }
    
    /**
     * Clear the list of discovered receivers
     */
    public void clearDiscoveredReceivers() {
        discoveredReceivers.clear();
    }
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            FileShareApp app = new FileShareApp();
            app.setVisible(true);
        });
    }
    
    /**
     * Inner class to store receiver information
     */
    public static class ReceiverInfo {
        private final String ip;
        private final int port;
        
        public ReceiverInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
        
        public String getIp() {
            return ip;
        }
        
        public int getPort() {
            return port;
        }
    }
}
