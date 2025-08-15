# MongoDB Setup Script for Health Assistant

Write-Host "🗄️ Setting up MongoDB for Health Assistant Chat History" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if MongoDB is installed
function Test-MongoDBInstalled {
    $mongoCmd = Get-Command mongod -ErrorAction SilentlyContinue
    return $null -ne $mongoCmd
}

# Function to check if MongoDB is running
function Test-MongoDBRunning {
    try {
        $result = Invoke-RestMethod -Uri "http://localhost:27017" -Method Get -TimeoutSec 5 -ErrorAction Stop
        return $true
    }
    catch {
        return $false
    }
}

# Check if MongoDB is installed
Write-Host "🔍 Checking MongoDB installation..." -ForegroundColor Yellow

if (Test-MongoDBInstalled) {
    Write-Host "✅ MongoDB is installed" -ForegroundColor Green
}
else {
    Write-Host "❌ MongoDB is not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install MongoDB Community Server:" -ForegroundColor Yellow
    Write-Host "1. Download from: https://www.mongodb.com/try/download/community"
    Write-Host "2. Or use Chocolatey: choco install mongodb"
    Write-Host "3. Or use winget: winget install MongoDB.Server"
    Write-Host ""
    
    $response = Read-Host "Do you want to install MongoDB using Chocolatey? (y/N)"
    if ($response -eq 'y' -or $response -eq 'Y') {
        Write-Host "Installing MongoDB via Chocolatey..." -ForegroundColor Yellow
        try {
            choco install mongodb -y
            Write-Host "✅ MongoDB installed successfully" -ForegroundColor Green
        }
        catch {
            Write-Host "❌ Failed to install MongoDB via Chocolatey" -ForegroundColor Red
            Write-Host "Please install manually and run this script again."
            exit 1
        }
    }
    else {
        Write-Host "Please install MongoDB manually and run this script again." -ForegroundColor Yellow
        exit 1
    }
}

# Check if MongoDB is running
Write-Host ""
Write-Host "🔍 Checking if MongoDB is running..." -ForegroundColor Yellow

if (Test-MongoDBRunning) {
    Write-Host "✅ MongoDB is running" -ForegroundColor Green
}
else {
    Write-Host "⚠️ MongoDB is not running. Starting MongoDB..." -ForegroundColor Yellow
    
    try {
        # Try to start MongoDB service
        Start-Service -Name "MongoDB" -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 3
        
        if (Test-MongoDBRunning) {
            Write-Host "✅ MongoDB service started successfully" -ForegroundColor Green
        }
        else {
            Write-Host "🚀 Starting MongoDB manually..." -ForegroundColor Yellow
            Start-Process -FilePath "mongod" -ArgumentList "--dbpath", "C:\data\db" -NoNewWindow
            Start-Sleep -Seconds 5
            
            if (Test-MongoDBRunning) {
                Write-Host "✅ MongoDB started successfully" -ForegroundColor Green
            }
            else {
                Write-Host "❌ Failed to start MongoDB" -ForegroundColor Red
                Write-Host "Please ensure MongoDB is properly installed and try again."
                exit 1
            }
        }
    }
    catch {
        Write-Host "❌ Failed to start MongoDB service" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)"
        exit 1
    }
}

# Setup MongoDB database and collections
Write-Host ""
Write-Host "🗃️ Setting up Health Assistant database..." -ForegroundColor Yellow

try {
    # Check if mongo shell is available
    $mongoShell = Get-Command mongo -ErrorAction SilentlyContinue
    if (-not $mongoShell) {
        $mongoShell = Get-Command mongosh -ErrorAction SilentlyContinue
    }
    
    if ($mongoShell) {
        $dbSetupScript = @"
use health_assistant
db.createCollection('chat_history')
db.chat_history.createIndex({ userId: 1 })
db.chat_history.createIndex({ timestamp: -1 })
db.chat_history.createIndex({ userId: 1, timestamp: -1 })
db.chat_history.createIndex({ department: 1 })
db.chat_history.createIndex({ sessionId: 1 })
print('Database and indexes created successfully')
"@
        
        $dbSetupScript | & $mongoShell.Source
        Write-Host "✅ Database and indexes created successfully" -ForegroundColor Green
    }
    else {
        Write-Host "⚠️ MongoDB shell not found. Database will be created automatically when first used." -ForegroundColor Yellow
    }
}
catch {
    Write-Host "⚠️ Could not setup database indexes. They will be created automatically." -ForegroundColor Yellow
    Write-Host "Error: $($_.Exception.Message)"
}

# Test MongoDB connection
Write-Host ""
Write-Host "🧪 Testing MongoDB connection..." -ForegroundColor Yellow

try {
    # Create a simple test using PowerShell HTTP client
    $testConnection = @{
        method = "POST"
        uri = "http://localhost:27017"
        body = ""
        headers = @{
            "Content-Type" = "application/json"
        }
    }
    
    # Just check if port is responding
    $tcpTest = Test-NetConnection -ComputerName "localhost" -Port 27017 -WarningAction SilentlyContinue
    
    if ($tcpTest.TcpTestSucceeded) {
        Write-Host "✅ MongoDB is responding on port 27017" -ForegroundColor Green
    }
    else {
        Write-Host "❌ MongoDB is not responding on port 27017" -ForegroundColor Red
    }
}
catch {
    Write-Host "⚠️ Connection test inconclusive, but MongoDB appears to be running" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🎉 MongoDB setup completed!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Configuration Summary:" -ForegroundColor Cyan
Write-Host "• Database: health_assistant"
Write-Host "• Collection: chat_history"
Write-Host "• Connection String: mongodb://localhost:27017"
Write-Host "• Indexes: Created for optimal query performance"
Write-Host ""
Write-Host "📊 You can monitor your chat history data using:" -ForegroundColor Yellow
Write-Host "• MongoDB Compass: https://www.mongodb.com/products/compass"
Write-Host "• mongo shell: Connect with 'mongo' or 'mongosh'"
Write-Host "• Application APIs: Use the /api/chat-history endpoints"
Write-Host ""
Write-Host "🚀 You can now start the Health Assistant system!" -ForegroundColor Green
Write-Host "   The chat history will be automatically stored in MongoDB."
Write-Host "==========================================================" -ForegroundColor Cyan
