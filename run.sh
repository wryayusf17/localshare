#!/bin/bash

# LocalShare Run Script

echo "==================================="
echo "LocalShare - LAN File Sharing"
echo "==================================="
echo ""

# Check if compiled
if [ ! -d "bin" ] || [ ! -f "bin/FileShareApp.class" ]; then
    echo "Application not compiled. Running build script..."
    ./build.sh
    echo ""
fi

# Run the application
echo "Starting LocalShare..."
java -cp bin FileShareApp
