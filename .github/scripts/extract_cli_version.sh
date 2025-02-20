#!/bin/bash

# Check if a parameter is provided
if [ -z "$1" ]; then
    echo "Error: No binary name provided. Usage: $0 <binary_name>"
    exit 1
fi

BINARY_NAME=$1

echo "Starting CLI version extraction for binary: $BINARY_NAME..."

# Find the installed JAR in Maven's local repository
JAR_PATH=$(find ~/.m2/repository/com/checkmarx/ast/ast-cli-java-wrapper/ -type f -name "ast-cli-java-wrapper-*.jar" ! -name "*-javadoc.jar" | sort -V | tail -n 1)

if [ -z "$JAR_PATH" ]; then
    echo "Error: ast-cli-java-wrapper JAR not found in Maven dependencies."
    exit 1
fi

echo "✅ Found JAR at: $JAR_PATH"

# Create a temporary directory to extract the CLI
TEMP_DIR=$(mktemp -d)
echo "Using temporary directory: $TEMP_DIR"

unzip -j "$JAR_PATH" "$BINARY_NAME" -d "$TEMP_DIR"
if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to unzip $BINARY_NAME from the JAR."
    exit 1
fi

if [ ! -f "$TEMP_DIR/$BINARY_NAME" ]; then
    echo "❌ Error: $BINARY_NAME not found inside the JAR."
    ls -la "$TEMP_DIR"
    exit 1
fi

chmod +x "$TEMP_DIR/$BINARY_NAME"

# Extract the CLI version
CLI_VERSION=$("$TEMP_DIR/$BINARY_NAME" version | grep -Eo '^[0-9]+\.[0-9]+\.[0-9]+')

if [ -z "$CLI_VERSION" ]; then
    echo "❌ Error: CLI_VERSION is not set or is empty."
    exit 1
fi

echo "✅ CLI version being packed is $CLI_VERSION"

# Export CLI version as an environment variable for GitHub Actions
echo "CLI_VERSION=$CLI_VERSION" >> $GITHUB_ENV

echo "🎉 CLI version extraction for $BINARY_NAME completed successfully!"
