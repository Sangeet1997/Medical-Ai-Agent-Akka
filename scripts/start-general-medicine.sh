#!/bin/bash

# Start General Medicine Node (Port 2551, HTTP 8080)
echo "Starting General Medicine Node..."
echo "Node will be available at:"
echo "  Akka Address: akka://HealthAssistantSystem@127.0.0.1:2551"
echo "  HTTP API: http://localhost:8080"
echo "  Roles: [general-medicine]"
echo ""

# Set JAVA_OPTS for better performance
export JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application with general medicine configuration
mvn exec:java -Dexec.mainClass="com.healthassistant.HealthAssistantApp" -Dexec.args="general-medicine"
