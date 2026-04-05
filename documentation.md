# **Project Documentation: Local LAN File Sharing Application**

## **1\. Project Overview**

Project Name: LocalShare Java Utility  
Goal: To develop a robust, cross-platform Java application that enables users to securely and reliably share files over a Local Area Network (LAN) using a two-phase socket communication model: UDP Broadcasting for discovery, and TCP Sockets for reliable data transfer.  
Target Platform: Java (JVM), designed for laptop/desktop environments.  
Inferred Grade Level: Advanced Undergraduate / Graduation Project (requires mastery of multi-threading, network I/O, and UI).

## **2\. Technical Requirements Checklist**

| ID | Requirement | Implementation Detail | Status |
| :---- | :---- | :---- | :---- |
| R1 | Must create a dedicated reception folder. | Handled by FileReceiver on startup/first receive. | **MANDATORY** |
| R2 | Must have "Receive" and "Send" buttons. | Simple Swing/JavaFX GUI for user interaction. | **MANDATORY** |
| R3 | Must use Sockets (TCP for transfer). | ServerSocket (Receiver) and Socket (Sender/Receiver). | **MANDATORY** |
| R4 | Must use Broadcast (UDP for discovery). | DatagramSocket (Sender/Receiver) on a specific port. | **MANDATORY** |
| R5 | Receiver broadcasts presence. | A listening thread in BroadcastDiscoverer on the Receiver side. | **MANDATORY** |
| R6 | Sender finds receiver, connects via TCP. | Sender receives UDP packet, extracts IP/Port, initiates TCP connection. | **MANDATORY** |

## **3\. Architectural Design**

The application will operate in two distinct modes (Sender/Receiver) and utilize two communication protocols. The core architecture relies heavily on multi-threading to ensure the GUI remains responsive while performing network operations.

### **A. Communication Protocols**

1. **Discovery Protocol (UDP Broadcast):**  
   * **Purpose:** Fast, stateless announcement of the receiver's presence and initial address exchange.  
   * **Receiver Action:** Continuously listen for incoming broadcast messages from potential Senders looking for a service. *Self-Correction: Based on the user workflow (Receiver broadcasts, Sender finds), the Receiver must actively broadcast its presence periodically.*  
     * **Message:** {"action": "PRESENCE", "ip": "192.168.1.5", "port": 5050} (JSON string or simple delimited string).  
     * **Port:** Dedicated UDP Broadcast Port (e.g., 8888).  
   * **Sender Action:** Listens for the presence messages on the broadcast port (8888) until a suitable target is found.  
2. **Transfer Protocol (TCP Socket):**  
   * **Purpose:** Reliable, connection-oriented file transfer.  
   * **Receiver Action:** A ServerSocket listens on a dedicated TCP Port (e.g., 5050). When connected, a new dedicated thread handles the file reception.  
   * **Sender Action:** Creates a Socket connection to the Receiver's IP and Port (extracted from the UDP message).

### **B. Core Class/Module Structure**

| Class Name | Responsibility | Key Components |
| :---- | :---- | :---- |
| FileShareApp | Main application entry point, GUI, and controller. | Swing/JavaFX components, main method. |
| FileReceiver | Manages the listening state and file saving. (R1, R3) | ServerSocket, Dedicated reception directory, File I/O Streams. |
| FileSender | Manages file selection and TCP transmission. (R3, R6) | Socket, File I/O Streams, File chooser dialog. |
| BroadcastService | Handles all UDP broadcast (Receiver announces) and listening (Sender discovers). (R4, R5) | DatagramSocket, DatagramPacket, Timer for periodic broadcast. |

## **4\. Implementation Steps for the AI Agent**

The agent must create four Java classes and implement the logic in sequential order.

### **Step 1: Initialization and GUI Setup (FileShareApp.java)**

1. Initialize a simple graphical interface (e.g., using Swing JFrame).  
2. Add two primary buttons: **"Send File"** and **"Start Receiving"**. (R2)  
3. Add a status log area (e.g., JTextArea) to display real-time events (e.g., "Broadcast started," "Found Receiver at X," "Transfer complete").  
4. Implement a state machine: The app should be either in SEND mode or RECEIVE mode, but not both concurrently.

### **Step 2: Receiver Implementation (FileReceiver.java)**

1. **Reception Folder (R1):** Define a constant for the receiving directory (LOCAL\_SHARE\_DIR \= "ReceivedFiles").  
   * In the constructor/initialization, check if this folder exists. If not, create it using Files.createDirectories() or new File().mkdirs().  
2. **TCP Listener (R3):** Create a Runnable or a dedicated thread to run a ServerSocket on a chosen port (e.g., 5050).  
3. **Connection Handling:** Inside the listener loop, call serverSocket.accept(). Upon connection:  
   * Spawn a new ReceiverHandler thread to process the incoming file stream.  
   * The main FileReceiver thread immediately returns to serverSocket.accept() to listen for the next connection.  
4. **File Stream Logic (ReceiverHandler):**  
   * Read the incoming TCP stream (socket.getInputStream()).  
   * **Protocol Parsing:** The first bytes of the stream should contain metadata:  
     * int: Length of the file name (N).  
     * byte\[N\]: File name string.  
     * long: File size (in bytes).  
   * Open a FileOutputStream using the received file name and the target directory (R1).  
   * Read the remaining bytes from the socket and write them directly to the FileOutputStream.  
   * Close all streams and the socket upon transfer completion or error.

### **Step 3: Broadcast Service Implementation (BroadcastService.java)**

1. **Discovery Ports:** Use a constant UDP Port (e.g., 8888).  
2. **Receiver Announcement (R5):**  
   * Create a DatagramSocket bound to the UDP port.  
   * Create a periodic task (using ScheduledExecutorService or a simple loop with Thread.sleep) to send a **PRESENCE** message (IP/Port of the TCP listener) to the network's broadcast address (e.g., 255.255.255.255).  
   * This task runs only when the "Start Receiving" button is active.  
3. **Sender Discovery (R6):**  
   * Create a separate thread that listens on the same UDP Port (8888).  
   * When a DatagramPacket is received, parse the **PRESENCE** message to extract the Receiver's IP address and TCP port number.  
   * Store the discovered addresses in a list/map in FileShareApp for the user to select.

### **Step 4: Sender Implementation (FileSender.java)**

1. **File Selection:** The "Send File" button in FileShareApp triggers:  
   * A JFileChooser to let the user select a file.  
   * A GUI prompt (or list) to select a discovered Receiver IP/Port from BroadcastService results.  
2. **TCP Connection (R6):** Create a Socket object connecting to the chosen Receiver's IP and TCP Port.  
3. **Transfer Logic:**  
   * Open a FileInputStream for the selected file.  
   * Open the OutputStream from the established socket.  
   * **Protocol Encoding:** Write the file metadata first (as defined in Step 2):  
     * Write the file name length (int).  
     * Write the file name bytes.  
     * Write the file size (long).  
   * Write the file content bytes from the FileInputStream to the socket's output stream in a loop (e.g., using a buffer of 4096 bytes).  
   * Display progress in the GUI.  
   * Close all streams and the socket upon transfer completion.

## **5\. Security and Robustness**

1. **Error Handling:** Implement try-catch-finally blocks extensively for all network and file I/O operations (R3, R4). Ensure sockets and file handles are closed reliably in the finally block to prevent resource leaks.  
2. **Multi-Threading:** All blocking network calls (accept(), read(), write(), receive(), send()) **must** be executed in background threads to prevent the GUI from freezing. Use SwingWorker or standard Thread/ExecutorService with proper thread synchronization.  
3. **Firewall Consideration:** Document that the application requires the specific TCP (e.g., 5050\) and UDP (e.g., 8888\) ports to be open in the host machine's firewall.

## **6\. Variables and Constants (To be defined in the code)**

| Constant Name | Value Example | Usage |
| :---- | :---- | :---- |
| TCP\_PORT | 5050 | Port for reliable file transfer. |
| UDP\_PORT | 8888 | Port for network discovery/broadcast. |
| BROADCAST\_ADDRESS | "255.255.255.255" | The general broadcast address. |
| LOCAL\_SHARE\_DIR | "./ReceivedFiles" | The local folder for saving received data. |
| BROADCAST\_INTERVAL | 5000 (ms) | How often the receiver announces its presence. |

This documentation provides all necessary components and logic to fulfill the project requirements using the specified discovery and transfer mechanisms. The key challenge for the implementation will be reliably handling the concurrent multi-threading required for both the server socket listener and the broadcast listener/sender.