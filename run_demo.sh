#!/bin/bash
# run_demo.sh -- Run the complete live demo of Kestrel Phase 3
echo "Building Kestrel..."
mkdir -p out
javac -d out src/kestrel/*.java
if [ $? -ne 0 ]; then
    echo "Build failed."
    exit 1
fi
echo ""
echo "Starting Complete Demonstration..."
echo ""
java -cp out kestrel.Demo
