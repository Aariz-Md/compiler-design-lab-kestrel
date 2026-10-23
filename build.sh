#!/bin/bash
# build.sh — Compile Kestrel into out/
echo "Compiling Kestrel..."
mkdir -p out
javac -d out src/kestrel/*.java
if [ $? -eq 0 ]; then
    echo "Build successful."
else
    echo "Build failed."
    exit 1
fi
