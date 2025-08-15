# Start Radiology Node (Port 2553, HTTP 8082)
Write-Host "Starting Radiology Node..." -ForegroundColor Cyan
Write-Host "Node will be available at:"
Write-Host "  Akka Address: akka://HealthAssistantSystem@127.0.0.1:2553"
Write-Host "  HTTP API: http://localhost:8082"
Write-Host "  Roles: [radiology]"
Write-Host ""

# Set JAVA_OPTS for better performance
$env:JAVA_OPTS = "-Xmx512m -Xms256m"

# Run the application with radiology configuration
mvn exec:java "-Dexec.mainClass=com.healthassistant.HealthAssistantApp" "-Dexec.args=radiology"
