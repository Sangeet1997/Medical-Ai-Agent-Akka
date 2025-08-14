#!/bin/bash

# Cluster monitoring script

echo "====================================="
echo "Health Assistant Cluster Monitor"
echo "====================================="
echo ""

# Function to check node status
check_node() {
    local port=$1
    local name=$2
    local akka_port=$3
    
    echo "Checking $name Node..."
    echo "  HTTP Port: $port"
    echo "  Akka Port: $akka_port"
    
    # Check HTTP endpoint
    if curl -s "http://localhost:$port/health" >/dev/null 2>&1; then
        echo "  ✅ HTTP Server: Running"
    else
        echo "  ❌ HTTP Server: Not responding"
    fi
    
    # Check if process is listening on Akka port
    if lsof -i ":$akka_port" >/dev/null 2>&1; then
        echo "  ✅ Akka Cluster: Running"
    else
        echo "  ❌ Akka Cluster: Not running"
    fi
    
    echo ""
}

# Check Ollama
echo "Checking Ollama Service..."
if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
    echo "  ✅ Ollama: Running at http://localhost:11434"
    
    # Check if LLaMA model is available
    if curl -s http://localhost:11434/api/tags | grep -q "llama3.2"; then
        echo "  ✅ LLaMA 3.2: Available"
    else
        echo "  ⚠️  LLaMA 3.2: Not found"
    fi
else
    echo "  ❌ Ollama: Not running"
fi
echo ""

# Check cluster nodes
check_node 8080 "General Medicine" 2551
check_node 8081 "Pharmacy" 2552  
check_node 8082 "Radiology" 2553

# Show cluster information if available
echo "====================================="
echo "Quick System Test"
echo "====================================="

if curl -s http://localhost:8080/health >/dev/null 2>&1; then
    echo "Testing basic functionality..."
    
    response=$(curl -s -X POST http://localhost:8080/query \
      -H "Content-Type: application/json" \
      -d '{
        "query": "Hello, this is a test",
        "userId": "monitor-test"
      }')
    
    if echo "$response" | grep -q "success"; then
        echo "✅ System is responding to queries"
    else
        echo "⚠️  System may have issues processing queries"
        echo "Response: $response"
    fi
else
    echo "❌ Cannot perform system test - no nodes responding"
fi

echo ""
echo "====================================="
echo "Monitoring completed"
echo ""
echo "To view live logs:"
echo "  tail -f logs/health-assistant.log"
echo ""
echo "To restart cluster:"
echo "  ./scripts/start-general-medicine.sh (Terminal 1)"
echo "  ./scripts/start-pharmacy.sh (Terminal 2)"  
echo "  ./scripts/start-radiology.sh (Terminal 3)"
echo "====================================="
