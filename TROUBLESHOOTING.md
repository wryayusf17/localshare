# LocalShare - Troubleshooting Guide

## Issue: "No Receivers Found"

### ✅ Solution Steps (Updated)

The code has been **fixed** to handle localhost testing better. Follow these exact steps:

---

## 🔧 Step-by-Step Fix

### Terminal 1 - RECEIVER

```bash
cd /home/wrya/Documents/github/localshare
./test_receiver.sh
```

**In the GUI:**
1. Click **"Start Receiving"**
2. **CHECK THE STATUS LOG** - You MUST see these messages:
   ```
   [HH:MM:SS] Receiving mode started. Broadcasting presence on network...
   [HH:MM:SS] TCP Server listening on port 5000
   [HH:MM:SS] Broadcasting presence every 2000ms on UDP port 8888
   [HH:MM:SS] DEBUG: Sent PRESENCE: LOCALSHARE_PRESENCE:192.168.0.123:5000
   ```

3. If you DON'T see "DEBUG: Sent PRESENCE", something is wrong - close and restart

---

### Terminal 2 - SENDER (Wait for receiver to start first!)

```bash
cd /home/wrya/Documents/github/localshare
./test_sender.sh
```

**In the GUI:**
1. Click **"Send File"**
2. Wait 3 seconds for discovery
3. **CHECK THE STATUS LOG** - You should see:
   ```
   [HH:MM:SS] Starting discovery to find receivers...
   [HH:MM:SS] Listening for receivers on UDP port 8888...
   [HH:MM:SS] DEBUG: Parsed PRESENCE from 192.168.0.123:5000
   [HH:MM:SS] Discovered receiver at 192.168.0.123:5000
   [HH:MM:SS] Discovery completed.
   ```

4. A dialog should appear with the receiver listed
5. Select it and choose a file to send

---

## 🔍 What Was Fixed

### Problem 1: Port Binding Conflict
**Before**: Sender couldn't bind to UDP port 8888 if receiver was using it
**Fixed**: Added `SO_REUSEADDR` option to allow multiple bindings

### Problem 2: Self-Filtering
**Before**: Application filtered out "self" IP addresses, preventing localhost testing
**Fixed**: Removed self-filtering to allow same-machine testing

### Problem 3: Lack of Debugging
**Before**: Hard to see what was happening
**Fixed**: Added DEBUG messages showing PRESENCE broadcasts and parsing

---

## 🧪 Quick Diagnostic

Run this to check if your network is working:

```bash
java -cp bin NetworkDiagnostic
```

**Expected Output:**
```
✓ UDP Port 8888 is AVAILABLE
✓ TCP Port 5000 is AVAILABLE
✓ Broadcast message sent successfully
```

---

## ❌ Common Errors & Solutions

### Error: "Address already in use"

**Cause**: Port 5000 or 8888 is in use

**Solution 1**: Close all LocalShare instances and wait 30 seconds

**Solution 2**: Kill the process
```bash
# Find what's using the port
sudo lsof -i :5000
sudo lsof -i :8888

# Kill it
sudo kill -9 <PID>
```

---

### Error: Still no receivers found after fixes

**Check 1**: Is receiver actually broadcasting?
- Look for "DEBUG: Sent PRESENCE" in receiver's status log
- Should appear every 2 seconds

**Check 2**: Is sender actually listening?
- Look for "Listening for receivers on UDP port 8888" in sender's log
- Should appear when you click "Send File"

**Check 3**: Firewall blocking?
```bash
# Temporarily disable firewall to test
sudo ufw disable

# Test the app

# Re-enable firewall
sudo ufw enable
sudo ufw allow 8888/udp
sudo ufw allow 5000/tcp
```

**Check 4**: Multiple network interfaces?
Your diagnostic showed:
- `192.168.0.123` (WiFi - your main network)
- `172.16.233.1` (VMware)
- `172.16.179.1` (VMware)

The app prefers `192.168.x.x` addresses. If both instances pick different interfaces, they won't find each other.

**Solution**: Disable VMware network adapters temporarily:
```bash
sudo ifconfig vmnet1 down
sudo ifconfig vmnet8 down
```

Then test again. Re-enable after:
```bash
sudo ifconfig vmnet1 up
sudo ifconfig vmnet8 up
```

---

## 📊 Verify It's Working

### Receiver Log Should Show:
```
[15:30:00] Application initialized. Ready to send or receive files.
[15:30:05] Receiving mode started. Broadcasting presence on network...
[15:30:05] Reception folder exists: /home/wrya/Documents/github/localshare/ReceivedFiles
[15:30:05] TCP Server listening on port 5000
[15:30:05] Broadcasting presence every 2000ms on UDP port 8888
[15:30:05] DEBUG: Sent PRESENCE: LOCALSHARE_PRESENCE:192.168.0.123:5000
[15:30:07] DEBUG: Sent PRESENCE: LOCALSHARE_PRESENCE:192.168.0.123:5000
[15:30:09] DEBUG: Sent PRESENCE: LOCALSHARE_PRESENCE:192.168.0.123:5000
```

### Sender Log Should Show:
```
[15:30:20] Application initialized. Ready to send or receive files.
[15:30:25] Starting discovery to find receivers...
[15:30:25] Listening for receivers on UDP port 8888...
[15:30:26] DEBUG: Parsed PRESENCE from 192.168.0.123:5000
[15:30:26] Discovered receiver at 192.168.0.123:5000
[15:30:28] Discovery completed.
```

---

## 🎯 If Still Not Working

### Ultimate Debug Test

1. **Close all LocalShare instances**

2. **Terminal 1 - Manual UDP Listener:**
   ```bash
   nc -ul 8888
   ```
   This listens for UDP broadcasts

3. **Terminal 2 - Run Receiver:**
   ```bash
   ./run.sh
   ```
   Click "Start Receiving"

4. **Check Terminal 1** - You should see broadcast messages appearing

5. If you DON'T see messages in Terminal 1, there's a network issue

---

## 📞 Report These If Still Failing

If it still doesn't work, run these commands and share the output:

```bash
# Network info
java -cp bin NetworkDiagnostic > diagnostic.txt

# Port status
netstat -tuln | grep -E "5000|8888" >> diagnostic.txt

# Firewall status
sudo ufw status >> diagnostic.txt

# Show the file
cat diagnostic.txt
```

---

## ✨ Expected Working Behavior

1. **Start Receiver**: Click "Start Receiving"
   - See "DEBUG: Sent PRESENCE" every 2 seconds
   
2. **Start Sender**: Click "Send File"
   - See "Listening for receivers..."
   - See "DEBUG: Parsed PRESENCE"
   - See "Discovered receiver"
   - Dialog pops up with receiver list

3. **Select & Send**: Choose receiver, pick file
   - See progress updates
   - File appears in ReceivedFiles/

**Transfer time**: ~0.5-1 second for small files (< 1MB)
