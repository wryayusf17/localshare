#!/bin/bash

echo "======================================================="
echo "LocalShare - RECEIVER Instance"
echo "======================================================="
echo ""
echo "This window will act as the RECEIVER."
echo ""
echo "INSTRUCTIONS:"
echo "1. When the application window opens, click 'Start Receiving'"
echo "2. Look for these messages in the status log:"
echo "   ✓ 'Receiving mode started'"
echo "   ✓ 'TCP Server listening on port 5050'"
echo "   ✓ 'Broadcasting presence...'"
echo "   ✓ 'DEBUG: Sent PRESENCE: LOCALSHARE_PRESENCE:...:5050'"
echo ""
echo "3. Keep this window open and running"
echo "4. In another terminal, run ./test_sender.sh to send files"
echo ""
echo "Press Enter to start LocalShare as RECEIVER..."
read

./run.sh
