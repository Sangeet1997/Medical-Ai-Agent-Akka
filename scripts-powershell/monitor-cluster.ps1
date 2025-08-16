# Cluster monitoring script

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Health Assistant Cluster Monitor" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Function to check node status
function Test-NodeStatus {
    param(
        [int]$Port,
        [string]$Name,
        [int]$AkkaPort
    )
    
    Write-Host "Checking $Name Node..."
    Write-Host "  HTTP Port: $Port"
    Write-Host "  Akka Port: $AkkaPort"
    
    # Check HTTP endpoint
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$Port/health" -TimeoutSec 5 -ErrorAction Stop
        Write-Host "  ✅ HTTP Server: Running" -ForegroundColor Green
    }
    catch {
        Write-Host "  ❌ HTTP Server: Not responding" -ForegroundColor Red
    }
    
    # Check if process is listening on Akka port
    $netstat = netstat -an | Select-String ":$AkkaPort "
    if ($netstat) {
        Write-Host "  ✅ Akka Cluster: Running" -ForegroundColor Green
    }
    else {
        Write-Host "  ❌ Akka Cluster: Not running" -ForegroundColor Red
    }
    
    Write-Host ""
}

# Check Ollama
Write-Host "Checking Ollama Service..."
try {
    $ollamaResponse = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "  ✅ Ollama: Running at http://localhost:11434" -ForegroundColor Green
}
catch {
    Write-Host "  ❌ Ollama: Not running" -ForegroundColor Red
}
Write-Host ""

# Check cluster nodes
Test-NodeStatus -Port 8080 -Name "General Medicine" -AkkaPort 2551
Test-NodeStatus -Port 8081 -Name "Pharmacy" -AkkaPort 2552
Test-NodeStatus -Port 8082 -Name "Radiology" -AkkaPort 2553

# Show cluster information if available
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Quick System Test" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

try {
    $healthCheck = Invoke-RestMethod -Uri "http://localhost:8080/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "Testing basic functionality..."
    
    $testQuery = @{
        query = "Hello, this is a test"
        userId = "monitor-test"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "http://localhost:8080/query" -Method Post -Body $testQuery -ContentType "application/json" -TimeoutSec 30
    
    if ($response -match "success" -or $response.response) {
        Write-Host "✅ System is responding to queries" -ForegroundColor Green
    }
    else {
        Write-Host "⚠️  System may have issues processing queries" -ForegroundColor Yellow
        Write-Host "Response: $($response | ConvertTo-Json)"
    }
}
catch {
    Write-Host "❌ Cannot perform system test - no nodes responding" -ForegroundColor Red
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Monitoring completed" -ForegroundColor Green
Write-Host ""
Write-Host "To view live logs:"
Write-Host "  Get-Content -Path logs\health-assistant.log -Wait"
Write-Host ""
Write-Host "To restart cluster:"
Write-Host "  .\scripts-powershell\start-general-medicine.ps1 (Terminal 1)"
Write-Host "  .\scripts-powershell\start-pharmacy.ps1 (Terminal 2)"
Write-Host "  .\scripts-powershell\start-radiology.ps1 (Terminal 3)"
Write-Host "=====================================" -ForegroundColor Cyan
