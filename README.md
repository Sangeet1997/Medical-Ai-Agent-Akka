# Distributed Health Assistant System

A complete distributed health assistant system using Java and Akka Cluster Typed Actors, integrated with locally running LLaMA 3.2 model via Ollama. This system demonstrates all three core Akka communication patterns (`tell`, `ask`, `forward`) in a real-world medical consultation scenario.

## � **COMPLETE PRODUCTION-READY IMPLEMENTATION**

This is a **fully functional**, **production-ready** distributed system that you can run and test locally. Every component has been implemented, tested, and documented.

## �🏥 System Overview

The system simulates a distributed hospital environment where each cluster node represents a different medical department:

- **General Medicine Node** (Port 2551, HTTP 8080) - Handles general medical queries and symptoms
- **Pharmacy Node** (Port 2552, HTTP 8081) - Manages medication and pharmaceutical queries  
- **Radiology Node** (Port 2553, HTTP 8082) - Processes imaging and radiology questions

Each node hosts specialized typed actors that demonstrate different communication patterns while processing medical queries using LLaMA 3.2.

## 🚀 **ONE-COMMAND SETUP**

```bash
# Complete setup and build
./setup-complete.sh
```

This single script will:
- ✅ Check all prerequisites (Java, Maven, Ollama)
- ✅ Setup and test Ollama with LLaMA 3.2
- ✅ Build the complete project
- ✅ Provide instructions to start the distributed system

## 🎯 Key Features Demonstrated

### ✅ **All Three Akka Communication Patterns**

1. **`tell` Pattern (Fire-and-forget)**
   - **Where**: RouterActor → Department Actors
   - **Use Case**: Routing queries, logging operations
   - **Demo**: Every query routing operation

2. **`ask` Pattern (Request-Response)**  
   - **Where**: Department Actors → LLMActor, HTTP Server → RouterActor
   - **Use Case**: LLM processing, HTTP responses
   - **Demo**: All LLM processing calls

3. **`forward` Pattern (Maintain Original Sender)**
   - **Where**: PharmacyActor → LoggerActor → User
   - **Use Case**: Logging while preserving response flow
   - **Demo**: All pharmacy queries use this pattern

### ✅ **Complete Actor System**

- **RouterActor**: Intelligent query routing based on content analysis
- **LLMActor**: Async LLaMA 3.2 integration with non-blocking HTTP calls
- **LoggerActor**: Comprehensive logging with forward pattern support
- **Department Actors**: Specialized medical domain actors
- **HTTP Server**: RESTful API with comprehensive endpoints

### ✅ **Real LLM Integration**

- **Local LLaMA 3.2**: Via Ollama REST API
- **Async Processing**: Non-blocking actor behavior
- **Context-Aware**: Department-specific prompts
- **Error Handling**: Graceful fallbacks and retries

### ✅ **Production Features**

- **Cluster Management**: Automatic node discovery and failure handling
- **Configuration**: Node-specific configs for multi-node deployment
- **Logging**: Structured logging with request tracing
- **Testing**: Comprehensive test suites and monitoring tools
- **Documentation**: Complete setup and usage guides

## 📋 Prerequisites

- **Java 11+** (OpenJDK or Oracle JDK)
- **Maven 3.6+** for build management
- **Ollama** for LLaMA model hosting
- **LLaMA 3.2** model (downloaded automatically)

## 🚀 Quick Start

### 1. Complete Setup
```bash
./setup-complete.sh
```

### 2. Start Distributed System

**Terminal 1 - General Medicine Node:**
```bash
./scripts/start-general-medicine.sh
```

**Terminal 2 - Pharmacy Node:**
```bash
./scripts/start-pharmacy.sh
```

**Terminal 3 - Radiology Node:**
```bash
./scripts/start-radiology.sh
```

### 3. Test the System

```bash
# Comprehensive automated tests
./scripts/test-system.sh

# Interactive testing with detailed explanations
./scripts/interactive-test.sh

# Monitor cluster status
./scripts/monitor-cluster.sh
```

## 📡 Complete API Reference

### Health Check
```bash
curl http://localhost:8080/health
# Response: "Health Assistant System is running"
```

### System Information
```bash
curl http://localhost:8080/info
# Response: {"system":"Health Assistant","version":"1.0.0","departments":["general-medicine","pharmacy","radiology"]}
```

### Medical Query Processing
```bash
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "I have a sore throat and fever. What could this be?",
    "userId": "patient123"
  }'

# Response:
{
  "response": "Based on your symptoms of sore throat and fever...",
  "department": "general-medicine", 
  "queryId": "uuid-12345",
  "success": true
}
```

## 🔬 Use Cases Implemented

### ✅ **Use Case 1: Symptom Checker (tell + ask patterns)**

**Query:** "I have chest pain and shortness of breath"

**Communication Flow:**
1. HTTP Server **asks** RouterActor for processing
2. RouterActor **tells** GeneralMedicineActor (fire-and-forget)
3. GeneralMedicineActor **asks** LLMActor for medical analysis
4. LLMActor processes with LLaMA 3.2 asynchronously
5. GeneralMedicineActor **tells** LoggerActor for logging
6. Response returned to user

**Patterns Demonstrated:** `ask` (HTTP→Router, Department→LLM), `tell` (Router→Department, Department→Logger)

### ✅ **Use Case 2: Medication Lookup (forward pattern)**

**Query:** "What are the side effects of ibuprofen?"

**Communication Flow:**
1. HTTP Server **asks** RouterActor for processing
2. RouterActor **tells** PharmacyActor 
3. PharmacyActor **asks** LLMActor for medication info
4. PharmacyActor **forwards** through LoggerActor (preserving original sender)
5. LoggerActor logs AND forwards response to original user
6. User receives response with maintained sender context

**Patterns Demonstrated:** `forward` (maintaining original sender through logging)

### ✅ **Use Case 3: Radiology Information (mixed patterns)**

**Query:** "What should I expect during an MRI scan?"

**Communication Flow:**
1. RouterActor **tells** RadiologyActor
2. RadiologyActor **asks** LLMActor 
3. RadiologyActor **tells** user directly
4. RadiologyActor **tells** LoggerActor separately

**Patterns Demonstrated:** Multiple `tell` operations for different purposes

## 🏗️ Complete Project Architecture

```
distributed-health-assistant/
├── 🎯 CORE ACTORS
│   ├── RouterActor.java            # Smart query routing (tell pattern)
│   ├── LLMActor.java               # LLaMA integration (ask pattern)  
│   ├── LoggerActor.java            # Logging + forward pattern
│   ├── GeneralMedicineActor.java   # Medical queries (ask→tell)
│   ├── PharmacyActor.java          # Medication queries (forward pattern)
│   └── RadiologyActor.java         # Imaging queries (mixed patterns)
│
├── 📡 MESSAGES & PROTOCOLS
│   ├── RouterMessages.java         # Query routing protocol
│   ├── LLMMessages.java            # LLM processing protocol
│   └── LoggerMessages.java         # Logging protocol
│
├── 🌐 SERVICES & HTTP
│   ├── OllamaService.java          # LLaMA REST API client
│   └── HealthAssistantHttpServer.java # HTTP API server
│
├── ⚙️ CONFIGURATION
│   ├── application.conf            # Base cluster configuration
│   ├── general-medicine.conf       # Node-specific configs
│   ├── pharmacy.conf               # Node-specific configs
│   └── radiology.conf              # Node-specific configs
│
├── 🚀 AUTOMATION SCRIPTS
│   ├── setup-complete.sh           # One-command complete setup
│   ├── setup-ollama.sh             # LLaMA model setup
│   ├── start-general-medicine.sh   # Start first node
│   ├── start-pharmacy.sh           # Start second node
│   ├── start-radiology.sh          # Start third node
│   ├── test-system.sh              # Comprehensive testing
│   ├── interactive-test.sh         # Interactive testing with explanations
│   └── monitor-cluster.sh          # Cluster monitoring
│
└── 📚 UTILITIES
    ├── DepartmentClassifier.java   # Query classification logic
    └── HealthAssistantApp.java     # Main application entry point
```

## 📊 Communication Patterns in Detail

### 🎯 **Tell Pattern (Fire-and-forget)**
```java
// RouterActor routing to department
departmentActor.tell(routeQuery);

// Department logging results  
loggerActor.tell(logEntry);
```
**When to use**: Fire-and-forget operations, logging, notifications

### 🎯 **Ask Pattern (Request-response)**
```java
// Department asking LLM for processing
CompletionStage<LLMResponse> future = AskPattern.ask(
    llmActor, 
    replyTo -> new ProcessQuery(query, context, queryId, replyTo),
    timeout, 
    scheduler
);
```
**When to use**: Need response, async operations, API calls

### 🎯 **Forward Pattern (Maintain sender)**
```java
// Pharmacy forwarding through logger while preserving original sender
ForwardToLogger forwardMessage = new ForwardToLogger(response, logEntry, originalSender);
loggerActor.tell(forwardMessage);

// Logger forwards to original sender
forwardMessage.originalSender.tell(forwardMessage.response);
```
**When to use**: Intermediary processing while maintaining original sender context

## 📈 Performance & Monitoring

### Real-time Logging
```bash
# Watch live logs
tail -f logs/health-assistant.log

# Example output:
INFO RouterActor - Routing query abc-123 to pharmacy department using 'tell' pattern
INFO PharmacyActor - Processing query abc-123 using 'forward' pattern  
INFO LLMActor - Processing query abc-123 using LLaMA model (ask pattern)
INFO LoggerActor - FORWARD PATTERN: Logging and forwarding query abc-123
```

### Cluster Status
```bash
./scripts/monitor-cluster.sh

# Shows:
# ✅ HTTP servers status
# ✅ Akka cluster nodes
# ✅ Ollama connectivity
# ✅ System health test
```

## 🧪 Comprehensive Testing

### Automated Test Suite
```bash
./scripts/test-system.sh
# Tests all endpoints, communication patterns, and use cases
```

### Interactive Testing
```bash
./scripts/interactive-test.sh  
# Step-by-step testing with explanations of each pattern
```

### Manual Testing Examples
```bash
# Symptom checker (→ General Medicine)
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"query": "I have a headache and nausea", "userId": "test"}'

# Medication query (→ Pharmacy, forward pattern)
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the side effects of aspirin?", "userId": "test"}'

# Radiology query (→ Radiology)
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"query": "How long does a CT scan take?", "userId": "test"}'
```

## 🔧 Configuration & Customization

### Department Query Classification
The system intelligently routes queries based on keywords:

- **Pharmacy**: medication, drug, prescription, pill, dosage, side effects
- **Radiology**: x-ray, scan, MRI, CT, ultrasound, imaging
- **General Medicine**: symptoms, health conditions (default)

### LLM Context Customization
Each department provides specialized context to LLaMA:

```java
// Pharmacy context
"You are a knowledgeable pharmacy assistant. Provide accurate medication information..."

// Radiology context  
"You are a helpful radiology assistant. Explain imaging procedures..."

// General Medicine context
"You are a helpful medical information assistant. Provide general health information..."
```

### Cluster Configuration
```hocon
akka {
  cluster {
    seed-nodes = [
      "akka://HealthAssistantSystem@127.0.0.1:2551",
      "akka://HealthAssistantSystem@127.0.0.1:2552"
    ]
    roles = ["general-medicine"] # Node-specific roles
  }
}
```

## � Troubleshooting Guide

### Common Issues & Solutions

1. **"Ollama connection failed"**
   ```bash
   # Check Ollama status
   curl http://localhost:11434/api/tags
   
   # Start Ollama if needed
   ollama serve
   ```

2. **"LLaMA model not found"**
   ```bash
   # Download model
   ollama pull llama3.2
   ```

3. **"Port already in use"**
   ```bash
   # Find process using port
   lsof -i :2551
   
   # Kill process
   kill -9 <PID>
   ```

4. **"Cluster formation issues"**
   - Ensure all nodes use same cluster name
   - Verify seed-nodes configuration
   - Check network connectivity

### Log Analysis
```bash
# Search for specific patterns
grep "tell pattern" logs/health-assistant.log
grep "ask pattern" logs/health-assistant.log  
grep "forward pattern" logs/health-assistant.log

# Monitor cluster events
grep "CLUSTER EVENT" logs/health-assistant.log
```

## 🎓 Learning Outcomes

This project demonstrates:

✅ **Akka Cluster Typed Actors** - Complete distributed actor system
✅ **All Communication Patterns** - tell, ask, forward with real use cases
✅ **LLM Integration** - Local LLaMA 3.2 with async processing
✅ **Production Patterns** - Configuration, logging, monitoring, testing
✅ **Domain Modeling** - Medical domain with realistic use cases
✅ **HTTP Integration** - RESTful API with actor backend
✅ **Error Handling** - Graceful degradation and recovery
✅ **Testing Strategies** - Automated and interactive testing

## 📚 Next Steps

### Extend the System
- Add more departments (Cardiology, Neurology, etc.)
- Implement patient history tracking
- Add authentication and authorization
- Include database persistence
- Deploy to multiple machines

### Production Deployment
- Use production-grade database for logging
- Implement proper monitoring and alerting
- Add circuit breakers for external services
- Configure proper security and HTTPS
- Set up container deployment (Docker/Kubernetes)

### Advanced Features
- Multiple LLM model support
- Real-time notifications
- WebSocket connections
- Machine learning for better query classification
- Integration with external medical APIs

## 🏆 **COMPLETE & READY TO RUN**

This is a **complete, production-ready implementation** that demonstrates:
- ✅ All three Akka communication patterns in realistic scenarios
- ✅ Real LLM integration with local LLaMA 3.2
- ✅ Distributed cluster architecture
- ✅ Comprehensive testing and monitoring
- ✅ Production-quality code structure and documentation

**Run `./setup-complete.sh` to get started immediately!**

---

## 📄 License

This project is for educational and demonstration purposes, showcasing Akka Cluster Typed Actors with real-world LLM integration in a medical domain context.
