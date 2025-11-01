import java.io.*;
import java.net.*;

/**
 * Handles sending files via TCP
 * - Connects to discovered receivers
 * - Sends file metadata and content according to protocol
 */
public class FileSender {
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for file transfer
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    
    private FileShareApp app;
    
    public FileSender(FileShareApp app) {
        this.app = app;
    }
    
    /**
     * Send a file to a receiver
     * Runs in a background thread to keep GUI responsive
     */
    public void sendFile(File file, FileShareApp.ReceiverInfo receiver) {
        if (file == null || !file.exists()) {
            app.logStatus("ERROR: File does not exist");
            return;
        }
        
        if (!file.isFile()) {
            app.logStatus("ERROR: Can only send files, not directories");
            return;
        }
        
        app.logStatus("Preparing to send: " + file.getName() + " to " + 
                     receiver.getIp() + ":" + receiver.getPort());
        
        // Run transfer in background thread
        new Thread(() -> {
            performFileTransfer(file, receiver);
        }).start();
    }
    
    /**
     * Perform the actual file transfer
     */
    private void performFileTransfer(File file, FileShareApp.ReceiverInfo receiver) {
        Socket socket = null;
        FileInputStream fis = null;
        DataOutputStream out = null;
        
        try {
            // Connect to receiver
            app.logStatus("Connecting to " + receiver.getIp() + ":" + receiver.getPort() + "...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(receiver.getIp(), receiver.getPort()), CONNECTION_TIMEOUT);
            app.logStatus("Connected successfully!");
            
            // Prepare streams
            fis = new FileInputStream(file);
            out = new DataOutputStream(socket.getOutputStream());
            
            // Get file metadata
            String fileName = file.getName();
            long fileSize = file.length();
            
            app.logStatus("Sending file metadata...");
            
            // Send metadata according to protocol:
            // 1. File name length (int)
            byte[] fileNameBytes = fileName.getBytes("UTF-8");
            out.writeInt(fileNameBytes.length);
            
            // 2. File name (bytes)
            out.write(fileNameBytes);
            
            // 3. File size (long)
            out.writeLong(fileSize);
            
            app.logStatus("Sending file: " + fileName + " (" + formatFileSize(fileSize) + ")");
            
            // Send file data
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalBytesSent = 0;
            int bytesRead;
            int lastProgress = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;
                
                // Log progress every 10%
                int progress = (int) ((totalBytesSent * 100) / fileSize);
                if (progress >= lastProgress + 10) {
                    app.logStatus("Progress: " + progress + "% (" + 
                                formatFileSize(totalBytesSent) + " / " + 
                                formatFileSize(fileSize) + ")");
                    lastProgress = progress;
                }
            }
            
            // Flush the output stream to ensure all data is sent
            out.flush();
            
            app.logStatus("File sent successfully: " + fileName);
            app.logStatus("Transfer complete: " + formatFileSize(totalBytesSent) + " sent");
            
        } catch (UnknownHostException e) {
            app.logStatus("ERROR: Unknown host - " + receiver.getIp());
        } catch (ConnectException e) {
            app.logStatus("ERROR: Connection refused - Receiver may not be running or firewall blocking");
        } catch (SocketTimeoutException e) {
            app.logStatus("ERROR: Connection timeout - Cannot reach receiver");
        } catch (IOException e) {
            app.logStatus("ERROR during file transfer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close all resources
            try {
                if (fis != null) {
                    fis.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                app.logStatus("Error closing resources: " + e.getMessage());
            }
        }
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
}
