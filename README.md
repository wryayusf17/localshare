# LocalShare - LAN File Sharing Application

A robust, cross-platform Java application for securely sharing files over a Local Area Network (LAN) using UDP broadcasting for discovery and TCP sockets for reliable data transfer.

## Features

- 🔍 **Automatic Discovery**: Receivers broadcast their presence on the network using UDP
- 📁 **File Transfer**: Reliable TCP-based file transfer with progress tracking
- 🖥️ **Simple GUI**: Easy-to-use Swing interface with Send and Receive buttons
- 📊 **Real-time Status**: Activity log showing all operations and progress
- 🔒 **Duplicate Handling**: Automatically renames files if they already exist
- 💪 **Multi-threaded**: Non-blocking operations keep the GUI responsive

## Architecture

### Communication Protocols

1. **UDP Broadcasting (Port 8888)**: Discovery mechanism
   - Receiver broadcasts PRESENCE messages every 2 seconds
   - Sender listens for broadcasts to find available receivers

2. **TCP Socket (Port 5050)**: File transfer
   - Reliable, connection-oriented data transfer
   - File metadata protocol: filename length → filename → file size → file data

### Components

- **FileShareApp**: Main GUI application and controller
- **BroadcastService**: Handles UDP discovery (both broadcasting and listening)
- **FileReceiver**: TCP server that receives files and saves to `ReceivedFiles/` folder
- **FileSender**: TCP client that connects and sends files to receivers

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Network connectivity (LAN)
- Firewall configuration to allow:
  - UDP port 8888 (broadcasting)
  - TCP port 5050 (file transfer)

## Compilation

### Linux/macOS

```bash
cd /home/wrya/Documents/github/localshare
javac -d bin src/*.java
```

### Windows

```cmd
cd C:\path\to\localshare
javac -d bin src\*.java
```

## Running the Application

### Linux/macOS

```bash
cd /home/wrya/Documents/github/localshare
java -cp bin FileShareApp
```

### Windows

```cmd
cd C:\path\to\localshare
java -cp bin FileShareApp
```

## Usage

### To Receive Files

1. Click the **"Start Receiving"** button
2. The application will:
   - Start listening for incoming files on TCP port 5050
   - Broadcast its presence on the network every 2 seconds
   - Create a `ReceivedFiles` folder if it doesn't exist
3. Wait for incoming files
4. Click **"Stop Receiving"** when done

### To Send Files

1. Ensure at least one receiver is running on the network
2. Click the **"Send File"** button
3. The application will:
   - Listen for 3 seconds to discover available receivers
   - Show a list of discovered receivers
4. Select a receiver from the list
5. Choose a file to send using the file chooser dialog
6. Monitor the transfer progress in the status log

## File Protocol

The application uses a custom protocol for file transfer:

```
[4 bytes] File name length (int)
[N bytes] File name (UTF-8 encoded)
[8 bytes] File size (long)
[M bytes] File data
```

## Network Configuration

### Firewall Rules

Make sure to allow the following ports through your firewall:

**Linux (ufw)**:
```bash
sudo ufw allow 8888/udp
sudo ufw allow 5050/tcp
```

**Windows Firewall**:
- Go to Windows Defender Firewall → Advanced Settings
- Create inbound rules for:
  - UDP port 8888
  - TCP port 5050

**macOS**:
```bash
# Firewall typically allows local network traffic by default
# If issues occur, check System Preferences → Security & Privacy → Firewall
```

## Project Structure

```
localshare/
├── documentation.md          # Detailed project documentation
├── README.md                # This file
├── src/
│   ├── FileShareApp.java    # Main GUI application
│   ├── BroadcastService.java # UDP discovery service
│   ├── FileReceiver.java    # TCP file receiver
│   └── FileSender.java      # TCP file sender
├── bin/                     # Compiled classes (created after compilation)
└── ReceivedFiles/          # Received files folder (created at runtime)
```

## Troubleshooting

### No receivers found when sending

- Ensure the receiver has clicked "Start Receiving"
- Check that both devices are on the same network
- Verify firewall settings allow UDP port 8888
- Try disabling firewall temporarily to test

### Connection refused during transfer

- Verify TCP port 5050 is open on the receiver
- Check that the receiver is still in "receiving" mode
- Ensure no other application is using port 5050

### Files not appearing in ReceivedFiles folder

- Check the application's status log for errors
- Verify disk space is available
- Check folder permissions

## Technical Details

- **Buffer Size**: 8KB for optimal transfer speed
- **Broadcast Interval**: 2 seconds
- **Discovery Timeout**: 3 seconds
- **Connection Timeout**: 5 seconds
- **Thread Pool**: Cached thread pool for handling multiple concurrent transfers

## Requirements Met

✅ R1: Dedicated reception folder (`ReceivedFiles/`)  
✅ R2: "Receive" and "Send" buttons in GUI  
✅ R3: TCP sockets for reliable file transfer  
✅ R4: UDP broadcast for network discovery  
✅ R5: Receiver broadcasts presence periodically  
✅ R6: Sender discovers receiver and connects via TCP

## License

This project is for educational purposes.

## Future Enhancements

- [ ] Support for multiple file transfers
- [ ] Directory transfer support
- [ ] Transfer encryption
- [ ] Transfer pause/resume functionality
- [ ] Custom port configuration via GUI
- [ ] Transfer speed limiting
- [ ] File transfer history
