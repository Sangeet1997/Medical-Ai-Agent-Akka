#!/bin/bash

# Start Pharmacy Node (Port 2552, HTTP 8081)
echo "Starting Pharmacy Node..."
echo "Node will be available at:"
echo "  Akka Address: akka://HealthAssistantSystem@127.0.0.1:2552"
echo "  HTTP API: http://localhost:8081"
echo "  Roles: [pharmacy]"
echo ""

# Set JAVA_OPTS for better performance
export JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application with pharmacy configuration
mvn exec:java -Dexec.mainClass="com.healthassistant.HealthAssistantApp" -Dexec.args="pharmacy"
