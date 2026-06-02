#!/bin/sh
set -e

cd /sandbox/code

# Find the Java file
JAVA_FILE=$(find . -name "*.java" -type f | head -1)

if [ -z "$JAVA_FILE" ]; then
    echo "ERROR: No Java file found" >&2
    exit 1
fi

# Compile
echo "Compiling $JAVA_FILE..."
javac "$JAVA_FILE" 2>&1

if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed" >&2
    exit 1
fi

# Extract class name (without .java extension and path)
CLASS_NAME=$(basename "$JAVA_FILE" .java)

# Run with timeout
echo "Running $CLASS_NAME..."
timeout ${TIMEOUT:-10} java -cp . "$CLASS_NAME"
