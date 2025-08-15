# Interactive test script for the Health Assistant System

$BASE_URL = "http://localhost:8080"

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Health Assistant Interactive Test Console" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script demonstrates all three Akka communication patterns:"
Write-Host "• tell: Router → Department Actors (fire-and-forget)"
Write-Host "• ask: Department Actors → LLM Actor (request-response)"
Write-Host "• forward: Pharmacy Actor → Logger Actor (maintain sender)"
Write-Host ""

# Function to send query and show detailed info
function Send-Query {
    param(
        [string]$Query,
        [string]$UserId,
        [string]$ExpectedDepartment
    )
    
    Write-Host "==================================" -ForegroundColor Yellow
    Write-Host "Sending Query to: $ExpectedDepartment" -ForegroundColor Yellow
    Write-Host "Query: $Query"
    Write-Host "User: $UserId"
    Write-Host "==================================" -ForegroundColor Yellow
    
    # Send query and capture response
    $requestBody = @{
        query = $Query
        userId = $UserId
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/query" -Method Post -Body $requestBody -ContentType "application/json" -TimeoutSec 30
        
        # Display response
        Write-Host "Response:"
        $response | ConvertTo-Json -Depth 5 | Write-Host
        Write-Host ""
        Write-Host "🔍 Check the application logs to see:" -ForegroundColor Cyan
        Write-Host "  - RouterActor using 'tell' pattern to route to $ExpectedDepartment"
        Write-Host "  - ${ExpectedDepartment}Actor using 'ask' pattern with LLMActor"
        if ($ExpectedDepartment -eq "pharmacy") {
            Write-Host "  - PharmacyActor using 'forward' pattern through LoggerActor"
        }
        else {
            Write-Host "  - ${ExpectedDepartment}Actor using 'tell' pattern to LoggerActor"
        }
        Write-Host ""
        Read-Host "Press Enter to continue"
        Write-Host ""
    }
    catch {
        Write-Host "Error sending query: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Read-Host "Press Enter to continue"
        Write-Host ""
    }
}

# Test 1: General Medicine Query (tell + ask patterns)
Send-Query -Query "I have been experiencing chest pain and shortness of breath. Should I be worried?" -UserId "patient-001" -ExpectedDepartment "general-medicine"

# Test 2: Pharmacy Query (tell + ask + forward patterns)
Send-Query -Query "What is the recommended dosage for ibuprofen and can I take it with blood pressure medication?" -UserId "patient-002" -ExpectedDepartment "pharmacy"

# Test 3: Radiology Query (tell + ask patterns)
Send-Query -Query "I need to get an MRI scan for my knee. What should I expect and how should I prepare?" -UserId "patient-003" -ExpectedDepartment "radiology"

# Test 4: Another General Medicine Query
Send-Query -Query "What are the early symptoms of diabetes and when should I see a doctor?" -UserId "patient-004" -ExpectedDepartment "general-medicine"

# Test 5: Another Pharmacy Query (demonstrates forward pattern again)
Send-Query -Query "Are there any side effects of taking aspirin daily for heart health?" -UserId "patient-005" -ExpectedDepartment "pharmacy"

# Test 6: Edge case - ambiguous query (should default to general medicine)
Send-Query -Query "I'm feeling tired and have trouble sleeping. Any advice?" -UserId "patient-006" -ExpectedDepartment "general-medicine"

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Interactive Testing Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary of Communication Patterns Demonstrated:"
Write-Host ""
Write-Host "🎯 TELL Pattern (Fire-and-forget):" -ForegroundColor Green
Write-Host "   • RouterActor → DepartmentActors (routing queries)"
Write-Host "   • DepartmentActors → LoggerActor (logging)"
Write-Host ""
Write-Host "🎯 ASK Pattern (Request-response):" -ForegroundColor Blue
Write-Host "   • HTTP Server → RouterActor (user requests)"
Write-Host "   • DepartmentActors → LLMActor (LLM processing)"
Write-Host ""
Write-Host "🎯 FORWARD Pattern (Maintain original sender):" -ForegroundColor Magenta
Write-Host "   • PharmacyActor → LoggerActor → User"
Write-Host "   • Logger handles logging AND forwards response"
Write-Host ""
Write-Host "Check the application logs to see detailed actor communication!"
Write-Host "===============================================" -ForegroundColor Cyan
