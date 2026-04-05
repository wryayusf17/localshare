# LocalShare - Quick Start Guide

## 🚀 Getting Started in 3 Steps

### 1. Build the Project

```bash
./build.sh
```

### 2. Run on Device A (Receiver)

```bash
./run.sh
```

Then click **"Start Receiving"** in the application window.

### 3. Run on Device B (Sender)

```bash
./run.sh
```

Then:
1. Click **"Send File"**
2. Select the receiver from the list
3. Choose a file to send

## 📋 Testing on a Single Machine

You can test the application on a single computer by:

1. Run the first instance and click "Start Receiving"
2. Run a second instance and click "Send File"
3. The sender will discover the receiver running on localhost
4. Select a file and watch it transfer!

**Note**: You may need to run the second instance from a different terminal/location to avoid conflicts.

## 🔥 Firewall Setup

### Linux (Ubuntu/Debian)
```bash
sudo ufw allow 8888/udp
sudo ufw allow 5000/tcp
```

### Check if ports are in use
```bash
# Check if TCP port 5050 is available
netstat -tuln | grep 5050

# Check if UDP port 8888 is available
netstat -tuln | grep 8888
```

## 📁 Where are my files?

Received files are saved in: `ReceivedFiles/` directory (created automatically)

## ⚠️ Troubleshooting

**Problem**: "No receivers found"
- **Solution**: Ensure the receiver has clicked "Start Receiving" and both devices are on the same network

**Problem**: "Connection refused"
- **Solution**: Check firewall settings and ensure TCP port 5050 is open

**Problem**: Compilation errors
- **Solution**: Ensure you have JDK 8 or higher installed (`java -version`)

## 🎯 Example Workflow

```
Device A (192.168.1.5):
$ ./run.sh
[Click "Start Receiving"]
> [15:30:42] Receiving mode started. Broadcasting presence on network...
> [15:30:42] TCP Server listening on port 5050

Device B (192.168.1.10):
$ ./run.sh
[Click "Send File"]
> [15:30:45] Starting discovery to find receivers...
> [15:30:46] Discovered receiver at 192.168.1.5:5050
[Select receiver, choose file "photo.jpg"]
> [15:30:50] Preparing to send: photo.jpg to 192.168.1.5:5050
> [15:30:51] Connected successfully!
> [15:30:51] Sending file: photo.jpg (2.45 MB)
> [15:30:52] Progress: 50% (1.23 MB / 2.45 MB)
> [15:30:53] File sent successfully: photo.jpg

Device A receives:
> [15:30:51] Incoming connection from 192.168.1.10
> [15:30:51] Receiving file: photo.jpg
> [15:30:51] File size: 2.45 MB
> [15:30:52] Progress: 50% (1.23 MB / 2.45 MB)
> [15:30:53] File received successfully: ReceivedFiles/photo.jpg
```

## 🔧 Advanced Usage

### Change Ports (requires code modification)

Edit these constants in the source files:
- UDP Discovery Port: `BroadcastService.java` → `UDP_PORT`
- TCP Transfer Port: `FileReceiver.java` → `TCP_PORT`

Then rebuild: `./build.sh`

### Create JAR file for easier distribution

```bash
cd bin
jar cvfe LocalShare.jar FileShareApp *.class
java -jar LocalShare.jar
```

## 📞 Support

Check the status log in the application window for detailed error messages and transfer progress.
