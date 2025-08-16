# Setup script for Ollama and LLaMA 3.2

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Health Assistant Setup Script" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# Check if Ollama is installed
$ollamaPath = Get-Command ollama -ErrorAction SilentlyContinue
if (-not $ollamaPath) {
    Write-Host "❌ Ollama is not installed." -ForegroundColor Red
    Write-Host "Please install Ollama first:"
    Write-Host "  Windows: Download from https://ollama.ai/download"
    Write-Host "  Or use: winget install Ollama.Ollama"
    exit 1
}

Write-Host "✅ Ollama is installed" -ForegroundColor Green

# Check if Ollama service is running
try {
    $response = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Ollama service is running" -ForegroundColor Green
}
catch {
    Write-Host "⚠️  Ollama service is not running." -ForegroundColor Yellow
    Write-Host "Starting Ollama service..."
    Start-Process -FilePath "ollama" -ArgumentList "serve" -NoNewWindow
    Write-Host "Ollama service started"
    
    # Wait a moment for service to start
    Start-Sleep -Seconds 3
}

# Check if LLaMA 3.2 model is available
Write-Host "Checking for LLaMA 3.2 model..."
$modelList = ollama list
if ($modelList -match "llama3.2") {
    Write-Host "✅ LLaMA 3.2 model is available" -ForegroundColor Green
}
else {
    Write-Host "⬇️  Downloading LLaMA 3.2 model (this may take a while)..." -ForegroundColor Yellow
    $result = ollama pull llama3.2
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ LLaMA 3.2 model downloaded successfully" -ForegroundColor Green
    }
    else {
        Write-Host "❌ Failed to download LLaMA 3.2 model" -ForegroundColor Red
        exit 1
    }
}

# Test Ollama with a simple query
Write-Host ""
Write-Host "Testing Ollama with LLaMA 3.2..."
$testPayload = @{
    model = "llama3.2"
    prompt = "Hello, respond with just the word OK"
    stream = $false
    options = @{
        temperature = 0.1
        max_tokens = 10
    }
} | ConvertTo-Json -Depth 3

try {
    $testResponse = Invoke-RestMethod -Uri "http://localhost:11434/api/generate" -Method Post -Body $testPayload -ContentType "application/json" -TimeoutSec 30
    if ($testResponse.response) {
        Write-Host "✅ Ollama LLaMA 3.2 test successful" -ForegroundColor Green
    }
    else {
        Write-Host "❌ Ollama LLaMA 3.2 test failed" -ForegroundColor Red
        Write-Host "Response: $($testResponse | ConvertTo-Json)"
        exit 1
    }
}
catch {
    Write-Host "❌ Ollama LLaMA 3.2 test failed" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
    exit 1
}

Write-Host ""
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "✅ Setup completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Ollama is running at: http://localhost:11434"
Write-Host "LLaMA 3.2 model is ready"
Write-Host ""
Write-Host "You can now start the Health Assistant nodes:"
Write-Host "  Terminal 1: .\scripts-powershell\start-general-medicine.ps1"
Write-Host "  Terminal 2: .\scripts-powershell\start-pharmacy.ps1"
Write-Host "  Terminal 3: .\scripts-powershell\start-radiology.ps1 (optional)"
Write-Host ""
Write-Host "Then test the system:"
Write-Host "  .\scripts-powershell\test-system.ps1"
Write-Host "===================================" -ForegroundColor Cyan
