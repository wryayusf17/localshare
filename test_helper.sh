#!/bin/bash

# LocalShare Test Helper Script

echo "==========================================="
echo "LocalShare - Two Instance Test Helper"
echo "==========================================="
echo ""
echo "This script helps you test LocalShare with clear instructions."
echo ""
echo "Current Setup:"
echo "  - Instance 1: Should be RECEIVING"
echo "  - Instance 2: Should be SENDING"
echo ""
echo "Instructions:"
echo ""
echo "1. In the FIRST window that opens:"
echo "   → Click 'Start Receiving'"
echo "   → Wait for 'Broadcasting presence' message"
echo ""
echo "2. In the SECOND window (this one):"
echo "   → Click 'Send File'"
echo "   → Select the receiver from the list"
echo "   → Choose a file to send"
echo ""
echo "Press Enter to continue..."
read

echo "Starting LocalShare..."
echo ""
java -cp bin FileShareApp
