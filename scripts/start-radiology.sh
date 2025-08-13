#!/bin/bash

# Start Radiology Node (Port 2553, HTTP 8082)
echo "Starting Radiology Node..."
echo "Node will be available at:"
echo "  Akka Address: akka://HealthAssistantSystem@127.0.0.1:2553"
echo "  HTTP API: http://localhost:8082"
echo "  Roles: [radiology]"
echo ""

# Set JAVA_OPTS for better performance
export JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application with radiology configuration
mvn exec:java -Dexec.mainClass="com.healthassistant.HealthAssistantApp" -Dexec.args="radiology"
