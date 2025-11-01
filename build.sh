#!/bin/bash

# LocalShare Build Script

echo "==================================="
echo "LocalShare - Build Script"
echo "==================================="

# Create bin directory if it doesn't exist
if [ ! -d "bin" ]; then
    echo "Creating bin directory..."
    mkdir bin
fi

# Compile Java files
echo "Compiling Java files..."
javac -d bin src/*.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful!"
    echo ""
    echo "To run the application, execute:"
    echo "  java -cp bin FileShareApp"
    echo ""
    echo "Or use the run script:"
    echo "  ./run.sh"
else
    echo "✗ Compilation failed!"
    exit 1
fi
