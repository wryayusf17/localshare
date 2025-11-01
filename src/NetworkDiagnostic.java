import java.net.*;
import java.io.*;

/**
 * Network diagnostic tool for LocalShare
 * Tests UDP broadcast and TCP connectivity
 */
public class NetworkDiagnostic {
    private static final int UDP_PORT = 8888;
    private static final int TCP_PORT = 5000;
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("LocalShare Network Diagnostic Tool");
        System.out.println("===========================================\n");
        
        // 1. Check local IP
        System.out.println("1. Local Network Information:");
        System.out.println("   -------------------------------------");
        checkLocalIP();
        
        // 2. Check UDP port availability
        System.out.println("\n2. UDP Port Check (Port " + UDP_PORT + "):");
        System.out.println("   -------------------------------------");
        checkUDPPort();
        
        // 3. Check TCP port availability
        System.out.println("\n3. TCP Port Check (Port " + TCP_PORT + "):");
        System.out.println("   -------------------------------------");
        checkTCPPort();
        
        // 4. Test UDP broadcast
        System.out.println("\n4. UDP Broadcast Test:");
        System.out.println("   -------------------------------------");
        testUDPBroadcast();
        
        // 5. Test UDP listening
        System.out.println("\n5. UDP Listening Test (5 seconds):");
        System.out.println("   -------------------------------------");
        testUDPListen();
        
        System.out.println("\n===========================================");
        System.out.println("Diagnostic Complete");
        System.out.println("===========================================");
    }
    
    private static void checkLocalIP() {
        try {
            System.out.println("   Hostname: " + InetAddress.getLocalHost().getHostName());
            System.out.println("   Default IP: " + InetAddress.getLocalHost().getHostAddress());
            
            System.out.println("\n   All Network Interfaces:");
            for (NetworkInterface iface : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                System.out.println("     Interface: " + iface.getName() + " (" + iface.getDisplayName() + ")");
                for (InetAddress addr : java.util.Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        System.out.println("       IPv4: " + addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
    }
    
    private static void checkUDPPort() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(UDP_PORT);
            System.out.println("   ✓ UDP Port " + UDP_PORT + " is AVAILABLE");
            socket.close();
        } catch (SocketException e) {
            System.out.println("   ✗ UDP Port " + UDP_PORT + " is IN USE");
            System.out.println("   Error: " + e.getMessage());
            System.out.println("   Solution: Close the other LocalShare instance or wait a moment");
        }
    }
    
    private static void checkTCPPort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(TCP_PORT);
            System.out.println("   ✓ TCP Port " + TCP_PORT + " is AVAILABLE");
            socket.close();
        } catch (IOException e) {
            System.out.println("   ✗ TCP Port " + TCP_PORT + " is IN USE");
            System.out.println("   Error: " + e.getMessage());
            System.out.println("   Solution: Close the receiver instance or wait a moment");
        }
    }
    
    private static void testUDPBroadcast() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            
            String message = "LOCALSHARE_TEST_BROADCAST";
            byte[] buffer = message.getBytes();
            
            InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddr, UDP_PORT);
            
            socket.send(packet);
            System.out.println("   ✓ Broadcast message sent successfully");
            System.out.println("   Message: " + message);
            System.out.println("   Target: 255.255.255.255:" + UDP_PORT);
            
        } catch (Exception e) {
            System.out.println("   ✗ Broadcast FAILED");
            System.out.println("   Error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    private static void testUDPListen() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(UDP_PORT);
            socket.setSoTimeout(5000); // 5 second timeout
            
            System.out.println("   Listening on UDP port " + UDP_PORT + "...");
            System.out.println("   Waiting for broadcasts (5 seconds)...");
            
            byte[] buffer = new byte[1024];
            int messagesReceived = 0;
            
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 5000) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("   ✓ Received: " + message);
                    System.out.println("     From: " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
                    messagesReceived++;
                    
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            
            if (messagesReceived == 0) {
                System.out.println("   ⚠ No broadcasts received");
                System.out.println("   This is normal if no receiver is running");
            } else {
                System.out.println("   ✓ Total messages received: " + messagesReceived);
            }
            
        } catch (SocketException e) {
            System.out.println("   ✗ Cannot bind to port " + UDP_PORT);
            System.out.println("   Error: " + e.getMessage());
            System.out.println("   Likely cause: Another LocalShare instance is running");
        } catch (Exception e) {
            System.out.println("   ✗ Listen test failed");
            System.out.println("   Error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
