#!/bin/bash

# Setup script for Ollama and LLaMA 3.2

echo "==================================="
echo "Health Assistant Setup Script"
echo "==================================="
echo ""

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama is not installed."
    echo "Please install Ollama first:"
    echo "  macOS: brew install ollama"
    echo "  Linux: curl -fsSL https://ollama.ai/install.sh | sh"
    echo "  Windows: Download from https://ollama.ai/download"
    exit 1
fi

echo "✅ Ollama is installed"

# Check if Ollama service is running
if ! curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
    echo "⚠️  Ollama service is not running."
    echo "Starting Ollama service..."
    ollama serve &
    OLLAMA_PID=$!
    echo "Ollama service started with PID: $OLLAMA_PID"
    
    # Wait a moment for service to start
    sleep 3
else
    echo "✅ Ollama service is running"
fi

# Check if LLaMA 3.2 model is available
echo "Checking for LLaMA 3.2 model..."
if ollama list | grep -q "llama3.2"; then
    echo "✅ LLaMA 3.2 model is available"
else
    echo "⬇️  Downloading LLaMA 3.2 model (this may take a while)..."
    ollama pull llama3.2
    if [ $? -eq 0 ]; then
        echo "✅ LLaMA 3.2 model downloaded successfully"
    else
        echo "❌ Failed to download LLaMA 3.2 model"
        exit 1
    fi
fi

# Test Ollama with a simple query
echo ""
echo "Testing Ollama with LLaMA 3.2..."
TEST_RESPONSE=$(curl -s -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.2",
    "prompt": "Hello, respond with just the word OK",
    "stream": false,
    "options": {
      "temperature": 0.1,
      "max_tokens": 10
    }
  }')

if echo "$TEST_RESPONSE" | grep -q "response"; then
    echo "✅ Ollama LLaMA 3.2 test successful"
else
    echo "❌ Ollama LLaMA 3.2 test failed"
    echo "Response: $TEST_RESPONSE"
    exit 1
fi

echo ""
echo "==================================="
echo "✅ Setup completed successfully!"
echo ""
echo "Ollama is running at: http://localhost:11434"
echo "LLaMA 3.2 model is ready"
echo ""
echo "You can now start the Health Assistant nodes:"
echo "  Terminal 1: ./scripts/start-general-medicine.sh"
echo "  Terminal 2: ./scripts/start-pharmacy.sh"
echo "  Terminal 3: ./scripts/start-radiology.sh (optional)"
echo ""
echo "Then test the system:"
echo "  ./scripts/test-system.sh"
echo "==================================="
