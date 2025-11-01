import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Handles receiving files via TCP
 * - Creates and manages the reception folder
 * - Listens for incoming TCP connections
 * - Receives file metadata and content
 */
public class FileReceiver {
    // Constants
    public static final int TCP_PORT = 5000;
    private static final String LOCAL_SHARE_DIR = "ReceivedFiles";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for file transfer
    
    private FileShareApp app;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private volatile boolean isReceiving = false;
    private Path receiveDirPath;
    
    public FileReceiver(FileShareApp app) {
        this.app = app;
        createReceptionFolder();
    }
    
    /**
     * Create the reception folder if it doesn't exist (R1)
     */
    private void createReceptionFolder() {
        try {
            receiveDirPath = Paths.get(LOCAL_SHARE_DIR);
            if (!Files.exists(receiveDirPath)) {
                Files.createDirectories(receiveDirPath);
                app.logStatus("Created reception folder: " + receiveDirPath.toAbsolutePath());
            } else {
                app.logStatus("Reception folder exists: " + receiveDirPath.toAbsolutePath());
            }
        } catch (IOException e) {
            app.logStatus("ERROR: Failed to create reception folder - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Start listening for incoming file transfers
     */
    public void startReceiving() throws IOException {
        if (isReceiving) {
            app.logStatus("Already receiving...");
            return;
        }
        
        isReceiving = true;
        serverSocket = new ServerSocket(TCP_PORT);
        executorService = Executors.newCachedThreadPool();
        
        app.logStatus("TCP Server listening on port " + TCP_PORT);
        
        // Start accept loop in background thread
        executorService.submit(() -> {
            while (isReceiving) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    app.logStatus("Incoming connection from " + clientSocket.getInetAddress().getHostAddress());
                    
                    // Handle each connection in a separate thread
                    executorService.submit(new ReceiverHandler(clientSocket));
                    
                } catch (SocketException e) {
                    if (isReceiving) {
                        app.logStatus("Server socket closed unexpectedly: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (isReceiving) {
                        app.logStatus("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * Stop listening for incoming file transfers
     */
    public void stopReceiving() {
        isReceiving = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            app.logStatus("Error closing server socket: " + e.getMessage());
        }
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
    
    /**
     * Handler for individual file reception connections
     */
    private class ReceiverHandler implements Runnable {
        private Socket socket;
        
        public ReceiverHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            DataInputStream in = null;
            FileOutputStream fos = null;
            
            try {
                in = new DataInputStream(socket.getInputStream());
                
                // Read file metadata according to protocol:
                // 1. File name length (int)
                int fileNameLength = in.readInt();
                app.logStatus("Receiving file name length: " + fileNameLength);
                
                // 2. File name (bytes)
                byte[] fileNameBytes = new byte[fileNameLength];
                in.readFully(fileNameBytes);
                String fileName = new String(fileNameBytes, "UTF-8");
                app.logStatus("Receiving file: " + fileName);
                
                // 3. File size (long)
                long fileSize = in.readLong();
                app.logStatus("File size: " + formatFileSize(fileSize));
                
                // Ensure unique filename if file already exists
                Path filePath = receiveDirPath.resolve(fileName);
                filePath = getUniqueFilePath(filePath);
                
                // Create file and start receiving data
                fos = new FileOutputStream(filePath.toFile());
                byte[] buffer = new byte[BUFFER_SIZE];
                long totalBytesRead = 0;
                int bytesRead;
                
                app.logStatus("Receiving file data...");
                
                while (totalBytesRead < fileSize) {
                    int toRead = (int) Math.min(BUFFER_SIZE, fileSize - totalBytesRead);
                    bytesRead = in.read(buffer, 0, toRead);
                    
                    if (bytesRead == -1) {
                        throw new IOException("Unexpected end of stream");
                    }
                    
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Log progress every 10%
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    if (progress % 10 == 0 && bytesRead > 0) {
                        app.logStatus("Progress: " + progress + "% (" + 
                                    formatFileSize(totalBytesRead) + " / " + 
                                    formatFileSize(fileSize) + ")");
                    }
                }
                
                app.logStatus("File received successfully: " + filePath.toAbsolutePath());
                app.logStatus("Transfer complete: " + fileName + " (" + formatFileSize(fileSize) + ")");
                
            } catch (EOFException e) {
                app.logStatus("ERROR: Incomplete file transfer - connection closed prematurely");
            } catch (IOException e) {
                app.logStatus("ERROR receiving file: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Close all resources
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (in != null) {
                        in.close();
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
         * Get a unique file path by adding numbers if file exists
         */
        private Path getUniqueFilePath(Path originalPath) {
            if (!Files.exists(originalPath)) {
                return originalPath;
            }
            
            String fileName = originalPath.getFileName().toString();
            String baseName;
            String extension;
            
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                baseName = fileName.substring(0, lastDot);
                extension = fileName.substring(lastDot);
            } else {
                baseName = fileName;
                extension = "";
            }
            
            int counter = 1;
            Path newPath;
            do {
                String newFileName = baseName + " (" + counter + ")" + extension;
                newPath = originalPath.getParent().resolve(newFileName);
                counter++;
            } while (Files.exists(newPath));
            
            return newPath;
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
}
