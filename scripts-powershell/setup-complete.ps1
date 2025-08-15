# Complete setup and start script for the Health Assistant System

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "🏥 Distributed Health Assistant System Setup" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This system demonstrates:"
Write-Host "• Akka Cluster Typed Actors"
Write-Host "• tell, ask, and forward communication patterns"
Write-Host "• LLaMA 3.2 integration via Ollama"
Write-Host "• Distributed medical query processing"
Write-Host ""

# Function to check prerequisites
function Test-Prerequisites {
    Write-Host "🔍 Checking prerequisites..." -ForegroundColor Yellow
    
    # Check Java
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        try {
            $javaVersionOutput = java -version 2>&1
            $javaVersion = ($javaVersionOutput | Select-String 'version' | Select-Object -First 1) -replace '.*"(.+?)".*', '$1'
            Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
        }
        catch {
            Write-Host "✅ Java: Found but version check failed" -ForegroundColor Green
        }
    }
    else {
        Write-Host "❌ Java not found. Please install Java 11 or higher." -ForegroundColor Red
        exit 1
    }
    
    # Check Maven
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        try {
            $mvnVersionOutput = mvn -version 2>&1
            $mvnVersion = ($mvnVersionOutput | Select-String 'Apache Maven' | Select-Object -First 1) -replace '.*Apache Maven (.+?) .*', '$1'
            Write-Host "✅ Maven: $mvnVersion" -ForegroundColor Green
        }
        catch {
            Write-Host "✅ Maven: Found but version check failed" -ForegroundColor Green
        }
    }
    else {
        Write-Host "❌ Maven not found. Please install Maven 3.6+." -ForegroundColor Red
        exit 1
    }
    
    # Check Ollama
    $ollamaCmd = Get-Command ollama -ErrorAction SilentlyContinue
    if ($ollamaCmd) {
        Write-Host "✅ Ollama is installed" -ForegroundColor Green
    }
    else {
        Write-Host "❌ Ollama not found. Please install Ollama first:" -ForegroundColor Red
        Write-Host "  Windows: Download from https://ollama.ai/download"
        Write-Host "  Or use: winget install Ollama.Ollama"
        exit 1
    }
    
    Write-Host ""
}

# Function to setup Ollama
function Initialize-Ollama {
    Write-Host "🚀 Setting up Ollama and LLaMA 3.2..." -ForegroundColor Yellow
    
    # Check if PowerShell script exists, fallback to bash script if needed
    $powershellScript = ".\scripts-powershell\setup-ollama.ps1"
    $bashScript = ".\scripts\setup-ollama.sh"
    
    if (Test-Path $powershellScript) {
        Write-Host "Running PowerShell setup script..." -ForegroundColor Cyan
        & $powershellScript
    }
    elseif (Test-Path $bashScript) {
        Write-Host "PowerShell script not found, trying bash script..." -ForegroundColor Yellow
        if (Get-Command bash -ErrorAction SilentlyContinue) {
            bash $bashScript
        }
        else {
            Write-Host "❌ Neither PowerShell script nor bash found. Please run setup manually." -ForegroundColor Red
            exit 1
        }
    }
    else {
        Write-Host "❌ Setup script not found. Please ensure setup scripts exist." -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

# Function to build project
function Build-Project {
    Write-Host "🔧 Building the project..." -ForegroundColor Yellow
    
    try {
        mvn clean compile
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Project built successfully" -ForegroundColor Green
        }
        else {
            Write-Host "❌ Build failed" -ForegroundColor Red
            exit 1
        }
    }
    catch {
        Write-Host "❌ Build failed with error: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

# Function to show startup instructions
function Show-StartupInstructions {
    $currentPath = Get-Location
    
    Write-Host "🎯 Starting the Health Assistant System..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "You need to open 3 PowerShell terminals and run these commands:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Terminal 1 (General Medicine Node):" -ForegroundColor Green
    Write-Host "  cd `"$currentPath`""
    Write-Host "  .\scripts-powershell\start-general-medicine.ps1"
    Write-Host ""
    Write-Host "Terminal 2 (Pharmacy Node):" -ForegroundColor Blue
    Write-Host "  cd `"$currentPath`""
    Write-Host "  .\scripts-powershell\start-pharmacy.ps1"
    Write-Host ""
    Write-Host "Terminal 3 (Radiology Node - Optional):" -ForegroundColor Magenta
    Write-Host "  cd `"$currentPath`""
    Write-Host "  .\scripts-powershell\start-radiology.ps1"
    Write-Host ""
    Write-Host "After all nodes are running, test the system:" -ForegroundColor Yellow
    Write-Host "  .\scripts-powershell\test-system.ps1"
    Write-Host ""
    Write-Host "Monitor the cluster:" -ForegroundColor Yellow
    Write-Host "  .\scripts-powershell\monitor-cluster.ps1"
    Write-Host ""
    Write-Host "Interactive testing:" -ForegroundColor Yellow
    Write-Host "  .\scripts-powershell\interactive-test.ps1"
    Write-Host ""
    Write-Host "Alternative: If PowerShell scripts don't exist, use bash scripts:" -ForegroundColor Gray
    Write-Host "  .\scripts\start-general-medicine.sh"
    Write-Host "  .\scripts\start-pharmacy.sh"
    Write-Host "  .\scripts\start-radiology.sh"
    Write-Host "  .\scripts\test-system.sh"
    Write-Host ""
}

# Function to show completion message
function Show-CompletionMessage {
    Write-Host "=======================================================" -ForegroundColor Cyan
    Write-Host "🎉 Setup completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📖 Quick Start Guide:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Start nodes in separate terminals (see commands above)"
    Write-Host "2. Wait for all nodes to join the cluster"
    Write-Host "3. Run tests to see the communication patterns in action"
    Write-Host ""
    Write-Host "📚 Communication Patterns Demonstrated:" -ForegroundColor Cyan
    Write-Host "• tell: RouterActor → Department Actors (fire-and-forget)" -ForegroundColor Green
    Write-Host "• ask:  Department Actors → LLMActor (request-response)" -ForegroundColor Blue
    Write-Host "• forward: PharmacyActor → LoggerActor (maintain sender)" -ForegroundColor Magenta
    Write-Host ""
    Write-Host "🔍 Monitor logs to see actor communication patterns!" -ForegroundColor Yellow
    Write-Host "  Log file: logs\health-assistant.log"
    Write-Host "  PowerShell: Get-Content -Path logs\health-assistant.log -Wait"
    Write-Host ""
    Write-Host "🌐 API Endpoints:" -ForegroundColor Cyan
    Write-Host "  General Medicine: http://localhost:8080"
    Write-Host "  Pharmacy:        http://localhost:8081"
    Write-Host "  Radiology:       http://localhost:8082"
    Write-Host ""
    Write-Host "🔧 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  • If execution policy errors occur:"
    Write-Host "    Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser"
    Write-Host "  • Check port usage: netstat -ano | findstr :8080"
    Write-Host "  • Restart Ollama if needed: ollama serve"
    Write-Host "=======================================================" -ForegroundColor Cyan
}

# Main execution function
function Start-HealthAssistantSetup {
    try {
        Test-Prerequisites
        Initialize-Ollama
        Build-Project
        Show-StartupInstructions
        Show-CompletionMessage
    }
    catch {
        Write-Host "❌ Setup failed with error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Stack trace: $($_.ScriptStackTrace)" -ForegroundColor Red
        exit 1
    }
}

# Run main function
Start-HealthAssistantSetup
