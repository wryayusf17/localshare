# LocalShare Architecture Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FileShareApp                            │
│                      (Main GUI Controller)                      │
│  ┌───────────────┐                        ┌──────────────────┐  │
│  │  Send Button  │                        │ Receive Button   │  │
│  └───────────────┘                        └──────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Status Log (Real-time Updates)              │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────┬─────────────────────────────────┬────────────────┘
               │                                 │
               │                                 │
       ┌───────▼────────┐                ┌──────▼───────┐
       │  FileSender    │                │ FileReceiver │
       │   (TCP Client) │                │(TCP Server)  │
       └───────┬────────┘                └──────┬───────┘
               │                                 │
               │         BroadcastService        │
               │    ┌────────────────────┐       │
               └────►  UDP Discovery     ◄───────┘
                    │  (Port 8888)       │
                    └────────────────────┘
```

## Communication Flow

### 1. Discovery Phase (UDP Broadcast - Port 8888)

```
Receiver (Device A):                    Sender (Device B):
┌──────────────┐                       ┌──────────────┐
│ Click "Start │                       │              │
│  Receiving"  │                       │              │
└──────┬───────┘                       │              │
       │                               │              │
       ▼                               │              │
┌──────────────────┐                   │              │
│ Start Broadcasting│                  │              │
│ PRESENCE messages│                   │              │
│ every 2 seconds  │                   │              │
└──────┬───────────┘                   │              │
       │                               │              │
       │  UDP Broadcast               │              │
       │  "192.168.1.5:5000"          │              │
       │ ─────────────────────────────►│              │
       │                               │  Click       │
       │                               │ "Send File"  │
       │                               └──────┬───────┘
       │                                      │
       │                                      ▼
       │                               ┌──────────────┐
       │  UDP Broadcast               │   Listen for  │
       │  "192.168.1.5:5000"          │   3 seconds   │
       │ ─────────────────────────────►│              │
       │                               └──────┬───────┘
       │                                      │
       │                                      ▼
       │                               ┌──────────────┐
       │                               │   Display    │
       │                               │   Receivers  │
       │                               └──────────────┘
```

### 2. File Transfer Phase (TCP Socket - Port 5000)

```
Sender:                                 Receiver:
┌──────────────┐                       ┌──────────────┐
│ User selects │                       │   Listening  │
│     file     │                       │   on TCP     │
└──────┬───────┘                       │   Port 5000  │
       │                               └──────┬───────┘
       ▼                                      │
┌──────────────┐                              │
│   Connect    │  TCP Connection              │
│ to Receiver  │ ─────────────────────────────►│
└──────┬───────┘                       ┌──────▼───────┐
       │                               │    Accept    │
       │                               │  Connection  │
       ▼                               └──────┬───────┘
┌──────────────┐                              │
│ Send Metadata│                              │
│ ────────────►│  [File Name Length (4B)]     │
│              │  [File Name (N bytes)]       │
│              │  [File Size (8B)]            │
│              │ ─────────────────────────────►│
└──────┬───────┘                       ┌──────▼───────┐
       │                               │    Parse     │
       │                               │   Metadata   │
       ▼                               └──────┬───────┘
┌──────────────┐                              │
│  Send File   │                              │
│    Data      │  [File Content Stream]       │
│  (8KB chunks)│ ─────────────────────────────►│
│              │                       ┌──────▼───────┐
│  Progress:   │                       │ Write to File│
│    10%...    │                       │ ReceivedFiles/│
│    50%...    │                       │   Progress:  │
│   100%       │                       │   10%...50%  │
└──────┬───────┘                       └──────┬───────┘
       │                                      │
       ▼                                      ▼
┌──────────────┐                       ┌──────────────┐
│   Transfer   │                       │    File      │
│   Complete   │                       │   Received!  │
└──────────────┘                       └──────────────┘
```

## Data Flow Protocol

### File Transfer Protocol (TCP)

```
┌────────────────────────────────────────────────────┐
│                 TCP Stream Format                  │
├────────────────────────────────────────────────────┤
│ 1. File Name Length (4 bytes - int)               │
│    Example: 0x00000009 (9 bytes)                  │
├────────────────────────────────────────────────────┤
│ 2. File Name (N bytes - UTF-8)                    │
│    Example: "photo.jpg"                           │
├────────────────────────────────────────────────────┤
│ 3. File Size (8 bytes - long)                     │
│    Example: 0x0000000000257C00 (2,457,600 bytes)  │
├────────────────────────────────────────────────────┤
│ 4. File Content (M bytes - binary)                │
│    [Actual file bytes in 8KB chunks]              │
│    [Progress tracked during transfer]             │
└────────────────────────────────────────────────────┘
```

## Thread Architecture

```
Main Thread (GUI - Event Dispatch Thread)
│
├─► BroadcastService Thread Pool
│   ├─► Broadcast Scheduler (when receiving)
│   │   └─► Sends UDP PRESENCE every 2 seconds
│   │
│   └─► Discovery Listener (when sending)
│       └─► Listens for UDP PRESENCE messages
│
├─► FileReceiver Thread Pool
│   ├─► ServerSocket Accept Loop
│   │   └─► Waits for incoming TCP connections
│   │
│   └─► ReceiverHandler Threads (one per connection)
│       ├─► Reads file metadata
│       ├─► Writes file to disk
│       └─► Reports progress to GUI
│
└─► FileSender Thread
    ├─► Connects to receiver
    ├─► Sends file metadata
    ├─► Sends file data
    └─► Reports progress to GUI
```

## Class Relationships

```
FileShareApp (JFrame)
│
├─► has-a BroadcastService
│   └─► manages DatagramSocket
│
├─► has-a FileReceiver
│   ├─► manages ServerSocket
│   └─► spawns ReceiverHandler threads
│
└─► has-a FileSender
    └─► creates Socket connections
```

## Network Topology Example

```
┌─────────────────────────────────────────────┐
│           Local Area Network (LAN)          │
│              192.168.1.0/24                 │
│                                             │
│  ┌──────────────┐         ┌──────────────┐ │
│  │  Device A    │         │  Device B    │ │
│  │ 192.168.1.5  │         │ 192.168.1.10 │ │
│  │              │         │              │ │
│  │ [Receiver]   │◄────────┤  [Sender]    │ │
│  │              │  TCP    │              │ │
│  │ Listening:   │  5000   │ Connects to: │ │
│  │ TCP :5000    │         │ 192.168.1.5  │ │
│  │              │         │    :5000     │ │
│  │ Broadcasting:│         │              │ │
│  │ UDP :8888    │◄────────┤  Listening:  │ │
│  │              │  UDP    │  UDP :8888   │ │
│  └──────────────┘  8888   └──────────────┘ │
│                                             │
└─────────────────────────────────────────────┘
```

## File System

```
localshare/
│
├── src/                    # Source code
│   ├── FileShareApp.java   # Main application & GUI
│   ├── BroadcastService.java  # UDP discovery
│   ├── FileReceiver.java   # TCP file receiver
│   └── FileSender.java     # TCP file sender
│
├── bin/                    # Compiled .class files
│   └── *.class
│
└── ReceivedFiles/          # Received files (auto-created)
    ├── photo.jpg
    ├── document.pdf
    └── ...
```
