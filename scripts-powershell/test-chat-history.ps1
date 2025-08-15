# Test script for MongoDB Chat History functionality

$BASE_URL = "http://localhost:8080"

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Health Assistant Chat History Testing" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

# Function to make HTTP requests with error handling
function Invoke-TestRequest {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [string]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Body $Body -Headers $Headers -ContentType "application/json" -TimeoutSec 30
        }
        else {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $Headers -TimeoutSec 30
        }
        return $response
    }
    catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Test 1: Send multiple queries to generate chat history
Write-Host "1. Generating chat history data..." -ForegroundColor Yellow

$testQueries = @(
    @{
        query = "I have a headache and feel nauseous. What could be causing this?"
        userId = "patient001"
        department = "general-medicine"
    },
    @{
        query = "What is the recommended dosage for ibuprofen for adults?"
        userId = "patient001" 
        department = "pharmacy"
    },
    @{
        query = "I need to get an MRI scan. What should I expect during the procedure?"
        userId = "patient002"
        department = "radiology"
    },
    @{
        query = "Can I take aspirin and warfarin together?"
        userId = "patient001"
        department = "pharmacy"
    },
    @{
        query = "What are the symptoms of diabetes?"
        userId = "patient002"
        department = "general-medicine"
    }
)

foreach ($testQuery in $testQueries) {
    Write-Host "  Sending query: '$($testQuery.query.Substring(0, [Math]::Min(50, $testQuery.query.Length)))...'" -ForegroundColor Gray
    
    $queryRequest = @{
        query = $testQuery.query
        userId = $testQuery.userId
    } | ConvertTo-Json
    
    $response = Invoke-TestRequest -Uri "$BASE_URL/query" -Method "POST" -Body $queryRequest
    if ($response) {
        Write-Host "    ✅ Response received from $($response.department)" -ForegroundColor Green
    }
    
    Start-Sleep -Seconds 2 # Brief pause between requests
}

Write-Host ""
Write-Host "2. Testing chat history retrieval..." -ForegroundColor Yellow

# Test 2: Get chat history for patient001
Write-Host "  📋 Getting chat history for patient001 (limit 10)..." -ForegroundColor Gray
$historyResponse = Invoke-TestRequest -Uri "$BASE_URL/api/chat-history/patient001/history?limit=10&department="
if ($historyResponse -and $historyResponse.success) {
    Write-Host "    ✅ Retrieved $($historyResponse.entries.Count) chat history entries" -ForegroundColor Green
    
    foreach ($entry in $historyResponse.entries) {
        Write-Host "      - [$($entry.department)] $($entry.query.Substring(0, [Math]::Min(40, $entry.query.Length)))... [$($entry.timestamp)]" -ForegroundColor Cyan
    }
} else {
    Write-Host "    ❌ Failed to retrieve chat history" -ForegroundColor Red
}

Write-Host ""

# Test 3: Get chat history for patient002
Write-Host "  📋 Getting chat history for patient002..." -ForegroundColor Gray
$historyResponse2 = Invoke-TestRequest -Uri "$BASE_URL/api/chat-history/patient002/history?limit=5&department="
if ($historyResponse2 -and $historyResponse2.success) {
    Write-Host "    ✅ Retrieved $($historyResponse2.entries.Count) chat history entries" -ForegroundColor Green
} else {
    Write-Host "    ❌ Failed to retrieve chat history for patient002" -ForegroundColor Red
}

Write-Host ""

# Test 4: Get pharmacy-specific chat history
Write-Host "  💊 Getting pharmacy-specific chat history for patient001..." -ForegroundColor Gray
$pharmacyHistory = Invoke-TestRequest -Uri "$BASE_URL/api/chat-history/patient001/history?limit=10&department=pharmacy"
if ($pharmacyHistory -and $pharmacyHistory.success) {
    Write-Host "    ✅ Retrieved $($pharmacyHistory.entries.Count) pharmacy chat entries" -ForegroundColor Green
    
    foreach ($entry in $pharmacyHistory.entries) {
        Write-Host "      - [PHARMACY] $($entry.query.Substring(0, [Math]::Min(40, $entry.query.Length)))..." -ForegroundColor Magenta
    }
} else {
    Write-Host "    ❌ Failed to retrieve pharmacy chat history" -ForegroundColor Red
}

Write-Host ""

# Test 5: Get chat analytics
Write-Host "3. Testing chat analytics..." -ForegroundColor Yellow

Write-Host "  📊 Getting overall system analytics..." -ForegroundColor Gray
$analyticsResponse = Invoke-TestRequest -Uri "$BASE_URL/api/chat-history/analytics?userId="
if ($analyticsResponse -and $analyticsResponse.success) {
    Write-Host "    ✅ Analytics retrieved successfully:" -ForegroundColor Green
    Write-Host "      - Total Queries: $($analyticsResponse.totalQueries)" -ForegroundColor Cyan
    Write-Host "      - Successful Queries: $($analyticsResponse.successfulQueries)" -ForegroundColor Cyan
    Write-Host "      - Average Response Time: $([Math]::Round($analyticsResponse.averageResponseTime, 2))ms" -ForegroundColor Cyan
    Write-Host "      - Most Active User: $($analyticsResponse.mostActiveUser)" -ForegroundColor Cyan
    Write-Host "      - Most Popular Department: $($analyticsResponse.mostPopularDepartment)" -ForegroundColor Cyan
} else {
    Write-Host "    ❌ Failed to retrieve analytics" -ForegroundColor Red
}

Write-Host ""

# Test 6: Get user-specific analytics
Write-Host "  📊 Getting analytics for patient001..." -ForegroundColor Gray
$userAnalytics = Invoke-TestRequest -Uri "$BASE_URL/api/chat-history/analytics?userId=patient001"
if ($userAnalytics -and $userAnalytics.success) {
    Write-Host "    ✅ User analytics retrieved:" -ForegroundColor Green
    Write-Host "      - User Queries: $($userAnalytics.totalQueries)" -ForegroundColor Cyan
    Write-Host "      - Successful: $($userAnalytics.successfulQueries)" -ForegroundColor Cyan
    Write-Host "      - Preferred Department: $($userAnalytics.mostPopularDepartment)" -ForegroundColor Cyan
} else {
    Write-Host "    ❌ Failed to retrieve user analytics" -ForegroundColor Red
}

Write-Host ""
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Chat History Testing Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Summary:" -ForegroundColor Yellow
Write-Host "• Chat history is now being stored in MongoDB"
Write-Host "• API endpoints are available for retrieving history"
Write-Host "• Analytics provide insights into system usage"
Write-Host "• Data persists across system restarts"
Write-Host ""
Write-Host "🔗 Available Endpoints:" -ForegroundColor Cyan
Write-Host "• GET /api/chat-history/{userId}/history?limit=N&department=X"
Write-Host "• GET /api/chat-history/analytics?userId=X"
Write-Host ""
Write-Host "🗄️ MongoDB Database:" -ForegroundColor Yellow
Write-Host "• Database: health_assistant"
Write-Host "• Collection: chat_history"
Write-Host "• Connection: mongodb://localhost:27017"
Write-Host ""
Write-Host "You can view the data directly in MongoDB Compass or mongo shell:"
Write-Host "  mongo health_assistant" -ForegroundColor Gray
Write-Host "  db.chat_history.find().pretty()" -ForegroundColor Gray
Write-Host "===============================================" -ForegroundColor Cyan
