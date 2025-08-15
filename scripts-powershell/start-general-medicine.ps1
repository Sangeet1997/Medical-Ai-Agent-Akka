# Start General Medicine Node (Port 2551, HTTP 8080)
Write-Host "Starting General Medicine Node..." -ForegroundColor Cyan
Write-Host "Node will be available at:"
Write-Host "  Akka Address: akka://HealthAssistantSystem@127.0.0.1:2551"
Write-Host "  HTTP API: http://localhost:8080"
Write-Host "  Roles: [general-medicine]"
Write-Host ""

# Set JAVA_OPTS for better performance
$env:JAVA_OPTS = "-Xmx512m -Xms256m"

# Run the application with general medicine configuration
mvn exec:java "-Dexec.mainClass=com.healthassistant.HealthAssistantApp" "-Dexec.args=general-medicine"
