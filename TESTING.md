# LocalShare - Testing Guide

## 🧪 Testing Checklist

### Prerequisites
- [ ] Java JDK 8+ installed (`java -version`)
- [ ] Project compiled successfully (`./build.sh`)
- [ ] Firewall configured (ports 5050 and 8888 open)

## Test Suite

### Test 1: Single Machine Test (Localhost)

**Objective**: Verify basic functionality on one computer

**Steps**:
1. Open Terminal 1 and run:
   ```bash
   cd /home/wrya/Documents/github/localshare
   ./run.sh
   ```

2. Click "Start Receiving" in the first window

3. Open Terminal 2 and run:
   ```bash
   cd /home/wrya/Documents/github/localshare
   ./run.sh
   ```

4. Click "Send File" in the second window

5. Select the receiver (should show 127.0.0.1:5050 or your local IP)

6. Choose a test file

**Expected Results**:
- ✓ Receiver discovered within 3 seconds
- ✓ File transfers successfully
- ✓ Progress updates visible in both windows
- ✓ File appears in `ReceivedFiles/` folder
- ✓ File content is identical to original

---

### Test 2: Network Test (Two Devices)

**Objective**: Test file sharing across network

**Device A (Receiver)**:
1. Run application: `./run.sh`
2. Click "Start Receiving"
3. Note the IP address shown in status log

**Device B (Sender)**:
1. Run application: `./run.sh`
2. Click "Send File"
3. Verify Device A appears in receiver list
4. Select Device A and choose a file

**Expected Results**:
- ✓ Both devices on same network
- ✓ Receiver broadcasts visible to sender
- ✓ TCP connection established
- ✓ File transfers successfully

---

### Test 3: Large File Transfer

**Objective**: Test with large files (>100MB)

**Steps**:
1. Create test file:
   ```bash
   dd if=/dev/urandom of=testfile_100mb.bin bs=1M count=100
   ```

2. Start receiver on Device A
3. Send `testfile_100mb.bin` from Device B

**Expected Results**:
- ✓ Progress updates show incremental progress
- ✓ Transfer completes without errors
- ✓ File sizes match (verify with `ls -lh`)
- ✓ File integrity preserved (verify with checksum):
   ```bash
   md5sum testfile_100mb.bin
   md5sum ReceivedFiles/testfile_100mb.bin
   ```

---

### Test 4: Multiple File Transfers

**Objective**: Test sequential file transfers

**Steps**:
1. Start receiver
2. Send file 1
3. Wait for completion
4. Send file 2
5. Send file 3

**Expected Results**:
- ✓ Each transfer completes successfully
- ✓ All files appear in ReceivedFiles/
- ✓ No connection errors between transfers

---

### Test 5: Duplicate File Handling

**Objective**: Verify duplicate file renaming

**Steps**:
1. Start receiver
2. Send `photo.jpg`
3. Send `photo.jpg` again
4. Send `photo.jpg` a third time

**Expected Results**:
- ✓ First file: `ReceivedFiles/photo.jpg`
- ✓ Second file: `ReceivedFiles/photo (1).jpg`
- ✓ Third file: `ReceivedFiles/photo (2).jpg`
- ✓ All files intact

---

### Test 6: Error Handling - No Receiver

**Objective**: Test sender behavior with no receivers

**Steps**:
1. Ensure no receivers are running
2. Run sender and click "Send File"
3. Wait for discovery timeout

**Expected Results**:
- ✓ "No receivers found" message appears
- ✓ Dialog shown to user
- ✓ Application remains stable

---

### Test 7: Error Handling - Connection Lost

**Objective**: Test behavior when connection interrupted

**Steps**:
1. Start receiver
2. Start sending a large file
3. Stop receiver mid-transfer (close application)

**Expected Results**:
- ✓ Sender detects connection error
- ✓ Error message logged
- ✓ Application doesn't crash
- ✓ Partial file may exist (expected)

---

### Test 8: Firewall Test

**Objective**: Verify firewall configuration

**Steps**:
1. **Block ports**:
   ```bash
   sudo ufw deny 8888/udp
   sudo ufw deny 5050/tcp
   ```

2. Try to discover receivers

3. **Open ports**:
   ```bash
   sudo ufw allow 8888/udp
   sudo ufw allow 5050/tcp
   ```

4. Try again

**Expected Results**:
- ✓ Discovery fails with closed ports
- ✓ Discovery succeeds with open ports

---

### Test 9: Special Characters in Filename

**Objective**: Test filenames with special characters

**Test Files**:
- `file with spaces.txt`
- `file-with-dashes.txt`
- `file_with_underscores.txt`
- `file.multiple.dots.txt`
- `файл.txt` (Unicode characters)

**Expected Results**:
- ✓ All files transfer successfully
- ✓ Filenames preserved correctly
- ✓ No encoding errors

---

### Test 10: Stress Test - Rapid Transfers

**Objective**: Test system under load

**Steps**:
1. Start receiver
2. Queue multiple files for transfer back-to-back
3. Send 10 files in quick succession

**Expected Results**:
- ✓ All files transfer successfully
- ✓ No connection refused errors
- ✓ No file corruption
- ✓ System remains responsive

---

## Performance Benchmarks

### Expected Transfer Speeds

| File Size | Expected Time* | Transfer Rate |
|-----------|---------------|---------------|
| 1 MB      | ~0.5 sec      | ~2 MB/s       |
| 10 MB     | ~5 sec        | ~2 MB/s       |
| 100 MB    | ~50 sec       | ~2 MB/s       |
| 1 GB      | ~8-10 min     | ~2 MB/s       |

*Times are approximate and depend on network conditions

### Measuring Transfer Speed

```bash
# Start receiver
./run.sh

# In another terminal, create test file and measure time
dd if=/dev/urandom of=testfile_10mb.bin bs=1M count=10
time ./send_test.sh testfile_10mb.bin
```

---

## Debugging Tools

### Check Network Connectivity

```bash
# Ping test between devices
ping 192.168.1.5

# Check if ports are accessible
nc -zv 192.168.1.5 5050  # TCP
nc -zuv 192.168.1.5 8888 # UDP
```

### Monitor Network Traffic

```bash
# Monitor UDP broadcasts
sudo tcpdump -i any -n udp port 8888

# Monitor TCP connections
sudo tcpdump -i any -n tcp port 5050
```

### Check Port Usage

```bash
# See what's using port 5050
sudo netstat -tulpn | grep 5050

# See what's using port 8888
sudo netstat -tulpn | grep 8888
```

---

## Common Issues & Solutions

### Issue: "Address already in use"
**Solution**: Another application is using port 5050 or 8888
```bash
# Find and kill the process
sudo lsof -i :5050
sudo kill -9 <PID>
```

### Issue: No receivers found
**Solutions**:
1. Check both devices are on same network
2. Verify receiver clicked "Start Receiving"
3. Check firewall settings
4. Try disabling firewall temporarily

### Issue: Connection refused
**Solutions**:
1. Verify TCP port 5050 is open
2. Check receiver is still running
3. Try restarting receiver

### Issue: Slow transfer speeds
**Solutions**:
1. Check network congestion
2. Test with smaller files first
3. Verify network connection quality
4. Close other network-intensive applications

---

## Test Report Template

```
Test Date: ______________
Tester: __________________
Java Version: ____________

Test Results:
[ ] Test 1: Single Machine - PASS / FAIL
[ ] Test 2: Network Transfer - PASS / FAIL
[ ] Test 3: Large Files - PASS / FAIL
[ ] Test 4: Multiple Transfers - PASS / FAIL
[ ] Test 5: Duplicate Handling - PASS / FAIL
[ ] Test 6: No Receiver - PASS / FAIL
[ ] Test 7: Connection Lost - PASS / FAIL
[ ] Test 8: Firewall - PASS / FAIL
[ ] Test 9: Special Chars - PASS / FAIL
[ ] Test 10: Stress Test - PASS / FAIL

Notes:
_______________________________________
_______________________________________
_______________________________________
```

---

## Automated Test Script

Create `test_basic.sh`:

```bash
#!/bin/bash

echo "LocalShare Basic Test Suite"
echo "==========================="

# Create test files
echo "Creating test files..."
echo "Hello World" > test_small.txt
dd if=/dev/urandom of=test_1mb.bin bs=1M count=1 2>/dev/null
dd if=/dev/urandom of=test_10mb.bin bs=1M count=10 2>/dev/null

# Calculate checksums
md5sum test_small.txt > checksums.txt
md5sum test_1mb.bin >> checksums.txt
md5sum test_10mb.bin >> checksums.txt

echo "Test files created!"
echo "Run receiver in one terminal, sender in another"
echo "After transfer, verify with:"
echo "  cd ReceivedFiles && md5sum -c ../checksums.txt"
```

---

## Success Criteria

✅ All 10 tests pass  
✅ No crashes or freezes  
✅ File integrity verified  
✅ Error messages clear and helpful  
✅ GUI remains responsive  
✅ Network discovery reliable  
✅ Transfer speeds acceptable  
✅ Resource cleanup proper (no memory leaks)
