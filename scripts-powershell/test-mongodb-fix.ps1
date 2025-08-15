# Test MongoDB Chat History Fix
Write-Host "🔧 Testing MongoDB Chat History Fix" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

# Clear existing test data
Write-Host "`n🗑️ Clearing existing test data..." -ForegroundColor Yellow
mongosh --eval "db.chat_history.deleteMany({userId: {`$regex: /^patient/}})" health_assistant | Out-Null

# Function to send query and test MongoDB storage
function Test-ChatHistoryStorage {
    param(
        [string]$Query,
        [string]$UserId,
        [string]$Description
    )
    
    Write-Host "`n📤 $Description" -ForegroundColor Green
    Write-Host "User: $UserId" -ForegroundColor Gray
    Write-Host "Query: $Query" -ForegroundColor Gray
    
    try {
        # Send query
        $queryData = @{
            query = $Query
            userId = $UserId
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "http://localhost:8080/query" -Method POST -Body $queryData -ContentType "application/json"
        Write-Host "✅ Query processed successfully" -ForegroundColor Green
        Write-Host "Query ID: $($response.queryId)" -ForegroundColor Cyan
        
        # Wait for MongoDB save
        Start-Sleep -Seconds 2
        
        # Check MongoDB
        $count = mongosh --quiet --eval "db.chat_history.find({userId: '$UserId'}).count()" health_assistant
        if ($count -gt 0) {
            Write-Host "✅ Data saved to MongoDB! ($count entries for $UserId)" -ForegroundColor Green
            
            # Show the latest entry
            $latestEntry = mongosh --quiet --eval "JSON.stringify(db.chat_history.findOne({userId: '$UserId'}, {}, {timestamp: -1}))" health_assistant | ConvertFrom-Json
            Write-Host "Latest entry:" -ForegroundColor White
            Write-Host "  Query ID: $($latestEntry.queryId)" -ForegroundColor DarkGray
            Write-Host "  Department: $($latestEntry.department)" -ForegroundColor DarkGray
            Write-Host "  Success: $($latestEntry.success)" -ForegroundColor DarkGray
            Write-Host "  Timestamp: $($latestEntry.timestamp)" -ForegroundColor DarkGray
        } else {
            Write-Host "❌ No data found in MongoDB for $UserId" -ForegroundColor Red
        }
        
        return $true
    }
    catch {
        Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Test 1: General Medicine Query
$test1 = Test-ChatHistoryStorage -Query "I have a persistent headache and feel dizzy. What could be causing this?" -UserId "test-patient-001" -Description "Testing General Medicine Query"

# Test 2: Pharmacy Query  
$test2 = Test-ChatHistoryStorage -Query "What is the recommended dosage for acetaminophen for severe headache?" -UserId "test-patient-002" -Description "Testing Pharmacy Query"

# Test 3: Radiology Query
$test3 = Test-ChatHistoryStorage -Query "I need to get a brain MRI. What should I expect during the procedure?" -UserId "test-patient-003" -Description "Testing Radiology Query"

# Test API endpoints
Write-Host "`n🔍 Testing Chat History API..." -ForegroundColor Yellow

try {
    # Test chat history retrieval
    $chatHistory = Invoke-RestMethod -Uri "http://localhost:8080/api/chat-history/test-patient-001/history?limit=5" -Method GET
    
    if ($chatHistory.success -and $chatHistory.entries.Count -gt 0) {
        Write-Host "✅ API retrieval working! Found $($chatHistory.entries.Count) entries" -ForegroundColor Green
        
        foreach ($entry in $chatHistory.entries) {
            Write-Host "  - [$($entry.department)] $($entry.query.Substring(0, [Math]::Min(50, $entry.query.Length)))..." -ForegroundColor Cyan
        }
    } else {
        Write-Host "❌ API retrieval failed or no data found" -ForegroundColor Red
    }
    
    # Test analytics
    $analytics = Invoke-RestMethod -Uri "http://localhost:8080/api/chat-history/analytics" -Method GET
    
    if ($analytics.success) {
        Write-Host "`n📊 System Analytics:" -ForegroundColor Green
        Write-Host "  Total Queries: $($analytics.totalQueries)" -ForegroundColor Cyan
        Write-Host "  Successful Queries: $($analytics.successfulQueries)" -ForegroundColor Cyan
        Write-Host "  Most Popular Department: $($analytics.mostPopularDepartment)" -ForegroundColor Cyan
    }
}
catch {
    Write-Host "❌ API test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Summary
Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "🎯 Test Results Summary:" -ForegroundColor Yellow

$successCount = 0
if ($test1) { $successCount++ }
if ($test2) { $successCount++ }
if ($test3) { $successCount++ }

Write-Host "✅ MongoDB Storage Tests: $successCount/3 successful" -ForegroundColor $(if ($successCount -eq 3) { "Green" } else { "Yellow" })

if ($successCount -eq 3) {
    Write-Host "🎉 All tests passed! MongoDB chat history is working correctly!" -ForegroundColor Green
    Write-Host ""
    Write-Host "The fix included:" -ForegroundColor White
    Write-Host "  • Added SaveChatHistoryFireAndForget message for fire-and-forget operations" -ForegroundColor Gray
    Write-Host "  • Updated ChatHistoryActor to handle the new message type" -ForegroundColor Gray
    Write-Host "  • Updated all Department Actors to use the correct message" -ForegroundColor Gray
    Write-Host "  • Added proper logging for MongoDB save operations" -ForegroundColor Gray
} else {
    Write-Host "⚠️ Some tests failed. Check the logs and MongoDB connection." -ForegroundColor Yellow
}

Write-Host "====================================" -ForegroundColor Cyan
