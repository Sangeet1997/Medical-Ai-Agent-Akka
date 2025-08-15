# Test script for the Health Assistant System

$BASE_URL = "http://localhost:8080"

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Health Assistant System Test Script" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
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

# Test 1: Health Check
Write-Host "1. Testing Health Check..."
$healthResponse = Invoke-TestRequest -Uri "$BASE_URL/health"
if ($healthResponse) {
    Write-Host $healthResponse -ForegroundColor Green
}
Write-Host ""
Write-Host ""

# Test 2: System Info
Write-Host "2. Testing System Info..."
$infoResponse = Invoke-TestRequest -Uri "$BASE_URL/info"
if ($infoResponse) {
    $infoResponse | ConvertTo-Json -Depth 5 | Write-Host
}
Write-Host ""
Write-Host ""

# Test 3: General Medicine Query (Symptom Checker)
Write-Host "3. Testing Symptom Checker (General Medicine)..."
$symptomQuery = @{
    query = "I have a sore throat and fever. What could this be?"
    userId = "test-user-001"
} | ConvertTo-Json
$symptomResponse = Invoke-TestRequest -Uri "$BASE_URL/query" -Method "POST" -Body $symptomQuery
if ($symptomResponse) {
    $symptomResponse | ConvertTo-Json -Depth 5 | Write-Host
}
Write-Host ""
Write-Host ""

# Test 4: Pharmacy Query (Medication Lookup)
Write-Host "4. Testing Medication Lookup (Pharmacy)..."
$pharmacyQuery = @{
    query = "What is ibuprofen used for and what are the side effects?"
    userId = "test-user-002"
} | ConvertTo-Json
$pharmacyResponse = Invoke-TestRequest -Uri "$BASE_URL/query" -Method "POST" -Body $pharmacyQuery
if ($pharmacyResponse) {
    $pharmacyResponse | ConvertTo-Json -Depth 5 | Write-Host
}
Write-Host ""
Write-Host ""

# Test 5: Radiology Query
Write-Host "5. Testing Radiology Query..."
$radiologyQuery = @{
    query = "What should I expect during an MRI scan?"
    userId = "test-user-003"
} | ConvertTo-Json
$radiologyResponse = Invoke-TestRequest -Uri "$BASE_URL/query" -Method "POST" -Body $radiologyQuery
if ($radiologyResponse) {
    $radiologyResponse | ConvertTo-Json -Depth 5 | Write-Host
}
Write-Host ""
Write-Host ""

# Test 6: General Medical Query
Write-Host "6. Testing General Medical Query..."
$generalQuery = @{
    query = "What are the symptoms of diabetes?"
    userId = "test-user-004"
} | ConvertTo-Json
$generalResponse = Invoke-TestRequest -Uri "$BASE_URL/query" -Method "POST" -Body $generalQuery
if ($generalResponse) {
    $generalResponse | ConvertTo-Json -Depth 5 | Write-Host
}
Write-Host ""
Write-Host ""

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Test completed!" -ForegroundColor Green
Write-Host "Check the logs for actor communication patterns:"
Write-Host "- tell: Router -> Department Actors"
Write-Host "- ask: Department Actors -> LLM Actor"
Write-Host "- forward: Pharmacy Actor -> Logger Actor"
Write-Host "===================================" -ForegroundColor Cyan
