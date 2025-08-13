#!/bin/bash

# Complete setup and start script for the Health Assistant System

echo "======================================================="
echo "🏥 Distributed Health Assistant System Setup"
echo "======================================================="
echo ""
echo "This system demonstrates:"
echo "• Akka Cluster Typed Actors"
echo "• tell, ask, and forward communication patterns"
echo "• LLaMA 3.2 integration via Ollama"
echo "• Distributed medical query processing"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo "🔍 Checking prerequisites..."
    
    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo "✅ Java: $JAVA_VERSION"
    else
        echo "❌ Java not found. Please install Java 11 or higher."
        exit 1
    fi
    
    # Check Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version 2>&1 | head -n 1 | awk '{print $3}')
        echo "✅ Maven: $MVN_VERSION"
    else
        echo "❌ Maven not found. Please install Maven 3.6+."
        exit 1
    fi
    
    # Check Ollama
    if command -v ollama &> /dev/null; then
        echo "✅ Ollama is installed"
    else
        echo "❌ Ollama not found. Please install Ollama first:"
        echo "  macOS: brew install ollama"
        echo "  Linux: curl -fsSL https://ollama.ai/install.sh | sh"
        exit 1
    fi
    
    echo ""
}

# Function to setup Ollama
setup_ollama() {
    echo "🚀 Setting up Ollama and LLaMA 3.2..."
    ./scripts/setup-ollama.sh
    echo ""
}

# Function to build project
build_project() {
    echo "🔧 Building the project..."
    mvn clean compile
    if [ $? -eq 0 ]; then
        echo "✅ Project built successfully"
    else
        echo "❌ Build failed"
        exit 1
    fi
    echo ""
}

# Function to start system
start_system() {
    echo "🎯 Starting the Health Assistant System..."
    echo ""
    echo "You need to open 3 terminals and run these commands:"
    echo ""
    echo "Terminal 1 (General Medicine Node):"
    echo "  cd $(pwd)"
    echo "  ./scripts/start-general-medicine.sh"
    echo ""
    echo "Terminal 2 (Pharmacy Node):"
    echo "  cd $(pwd)"
    echo "  ./scripts/start-pharmacy.sh"
    echo ""
    echo "Terminal 3 (Radiology Node - Optional):"
    echo "  cd $(pwd)"
    echo "  ./scripts/start-radiology.sh"
    echo ""
    echo "After all nodes are running, test the system:"
    echo "  ./scripts/test-system.sh"
    echo ""
    echo "Monitor the cluster:"
    echo "  ./scripts/monitor-cluster.sh"
    echo ""
    echo "Interactive testing:"
    echo "  ./scripts/interactive-test.sh"
    echo ""
}

# Main execution
main() {
    check_prerequisites
    setup_ollama
    build_project
    start_system
    
    echo "======================================================="
    echo "🎉 Setup completed successfully!"
    echo ""
    echo "📖 Quick Start Guide:"
    echo ""
    echo "1. Start nodes in separate terminals (see commands above)"
    echo "2. Wait for all nodes to join the cluster"
    echo "3. Run tests to see the communication patterns in action"
    echo ""
    echo "📚 Communication Patterns Demonstrated:"
    echo "• tell: RouterActor → Department Actors (fire-and-forget)"
    echo "• ask:  Department Actors → LLMActor (request-response)"
    echo "• forward: PharmacyActor → LoggerActor (maintain sender)"
    echo ""
    echo "🔍 Monitor logs to see actor communication patterns!"
    echo "======================================================="
}

# Run main function
main
