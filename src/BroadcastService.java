import java.net.*;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * Handles UDP broadcast for network discovery
 * - Receiver mode: Periodically broadcasts PRESENCE messages
 * - Sender mode: Listens for PRESENCE messages to discover receivers
 */
public class BroadcastService {
    // Constants
    private static final int UDP_PORT = 8888;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final int BROADCAST_INTERVAL = 2000; // 2 seconds
    private static final int BUFFER_SIZE = 1024;
    
    // Message protocol
    private static final String PRESENCE_PREFIX = "LOCALSHARE_PRESENCE:";
    
    private FileShareApp app;
    private DatagramSocket socket;
    private ScheduledExecutorService broadcastScheduler;
    private ExecutorService discoveryExecutor;
    private volatile boolean isBroadcasting = false;
    private volatile boolean isDiscovering = false;
    
    public BroadcastService(FileShareApp app) {
        this.app = app;
    }
    
    /**
     * Start broadcasting PRESENCE messages (Receiver mode)
     * @param actualTcpPort the actual TCP port the receiver is listening on
     */
    public void startBroadcasting(int actualTcpPort) throws IOException {
        if (isBroadcasting) {
            return;
        }
        
        isBroadcasting = true;
        broadcastScheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Start periodic broadcast
        broadcastScheduler.scheduleAtFixedRate(() -> {
            try {
                sendPresenceMessage(actualTcpPort);
            } catch (Exception e) {
                app.logStatus("Broadcast error: " + e.getMessage());
            }
        }, 0, BROADCAST_INTERVAL, TimeUnit.MILLISECONDS);
        
        app.logStatus("Broadcasting presence every " + BROADCAST_INTERVAL + "ms on UDP port " + UDP_PORT);
    }
    
    /**
     * Stop broadcasting PRESENCE messages
     */
    public void stopBroadcasting() {
        isBroadcasting = false;
        if (broadcastScheduler != null) {
            broadcastScheduler.shutdown();
            try {
                if (!broadcastScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    broadcastScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                broadcastScheduler.shutdownNow();
            }
        }
    }
    
    /**
     * Send a single PRESENCE message via UDP broadcast
     */
    private void sendPresenceMessage(int tcpPort) throws IOException {
        DatagramSocket tempSocket = null;
        try {
            tempSocket = new DatagramSocket();
            tempSocket.setBroadcast(true);
            
            // Get local IP address
            String localIp = getLocalIpAddress();
            String hostName = getLocalHostName();
            
            // Create message: "LOCALSHARE_PRESENCE:192.168.1.5:5050:DeviceName"
            String message = PRESENCE_PREFIX + localIp + ":" + tcpPort + ":" + hostName;
            byte[] buffer = message.getBytes();
            
            InetAddress broadcastAddr = InetAddress.getByName(BROADCAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddr, UDP_PORT);
            
            tempSocket.send(packet);
        } finally {
            if (tempSocket != null && !tempSocket.isClosed()) {
                tempSocket.close();
            }
        }
    }
    
    /**
     * Discover receivers on the network (Sender mode)
     * Listens for PRESENCE messages for a specified timeout
     */
    public void discoverReceivers(int timeoutMs) throws IOException {
        if (isDiscovering) {
            app.logStatus("Discovery already in progress...");
            return;
        }
        
        isDiscovering = true;
        app.clearDiscoveredReceivers();
        
        DatagramSocket discoverySocket = null;
        try {
            discoverySocket = new DatagramSocket(null);
            discoverySocket.setReuseAddress(true);
            discoverySocket.bind(new InetSocketAddress(UDP_PORT));
            discoverySocket.setBroadcast(true);
            discoverySocket.setSoTimeout(500); // Short per-receive timeout so elapsed time check works
            
            app.logStatus("Listening for receivers on UDP port " + UDP_PORT + "...");
            
            byte[] buffer = new byte[BUFFER_SIZE];
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    discoverySocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    parsePresenceMessage(message);
                    
                } catch (SocketTimeoutException e) {
                    // Short timeout expired, loop will check elapsed time
                    continue;
                }
            }
            
            app.logStatus("Discovery completed.");
            
        } finally {
            isDiscovering = false;
            if (discoverySocket != null && !discoverySocket.isClosed()) {
                discoverySocket.close();
            }
        }
    }
    
    /**
     * Parse a PRESENCE message and extract IP, port, and hostname
     * Format: "LOCALSHARE_PRESENCE:192.168.1.5:5050:DeviceName"
     */
    private void parsePresenceMessage(String message) {
        try {
            if (message.startsWith(PRESENCE_PREFIX)) {
                String data = message.substring(PRESENCE_PREFIX.length());
                String[] parts = data.split(":", 3);
                
                if (parts.length >= 2) {
                    String ip = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    String hostName = (parts.length >= 3 && !parts[2].isEmpty()) ? parts[2] : ip;
                    
                    // Always add discovered receivers (allow localhost for testing)
                    app.addDiscoveredReceiver(ip, port, hostName);
                }
            }
        } catch (Exception e) {
            app.logStatus("Failed to parse PRESENCE message: " + e.getMessage());
        }
    }
    
    /**
     * Get the local IP address of this machine
     */
    private String getLocalIpAddress() {
        try {
            // Try to find the actual network interface IP
            for (NetworkInterface iface : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                for (InetAddress addr : java.util.Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Prefer local network addresses
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
            
            // Fallback to default
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
    
    /**
     * Get the local hostname of this machine
     */
    private String getLocalHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            // Remove .local suffix common on macOS
            if (hostName.endsWith(".local")) {
                hostName = hostName.substring(0, hostName.length() - 6);
            }
            return hostName;
        } catch (Exception e) {
            return "Unknown Device";
        }
    }

    /**
     * Check if an IP address is a local address of this machine
     */
    private boolean isLocalAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            
            // Check if it's a loopback address
            if (addr.isLoopbackAddress()) {
                return true;
            }
            
            // Check all network interfaces
            for (NetworkInterface iface : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress ifaceAddr : java.util.Collections.list(iface.getInetAddresses())) {
                    if (ifaceAddr.equals(addr)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // If we can't determine, assume it's not local
        }
        
        return false;
    }
}
