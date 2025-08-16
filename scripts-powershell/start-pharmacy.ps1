# Start Pharmacy Node (Port 2552, HTTP 8081)
Write-Host "Starting Pharmacy Node..." -ForegroundColor Cyan
Write-Host "Node will be available at:"
Write-Host "  Akka Address: akka://HealthAssistantSystem@127.0.0.1:2552"
Write-Host "  HTTP API: http://localhost:8081"
Write-Host "  Roles: [pharmacy]"
Write-Host ""

# Set JAVA_OPTS for better performance
$env:JAVA_OPTS = "-Xmx512m -Xms256m"

# Run the application with pharmacy configuration
mvn exec:java "-Dexec.mainClass=com.healthassistant.HealthAssistantApp" "-Dexec.args=pharmacy"
