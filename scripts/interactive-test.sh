#!/bin/bash

# Interactive test script for the Health Assistant System

BASE_URL="http://localhost:8080"

echo "=============================================="
echo "Health Assistant Interactive Test Console"
echo "=============================================="
echo ""
echo "This script demonstrates all three Akka communication patterns:"
echo "• tell: Router → Department Actors (fire-and-forget)"
echo "• ask: Department Actors → LLM Actor (request-response)" 
echo "• forward: Pharmacy Actor → Logger Actor (maintain sender)"
echo ""

# Function to send query and show detailed info
send_query() {
    local query="$1"
    local user_id="$2"
    local expected_department="$3"
    
    echo "=================================="
    echo "Sending Query to: $expected_department"
    echo "Query: $query"
    echo "User: $user_id"
    echo "=================================="
    
    # Send query and capture response
    response=$(curl -s -X POST "$BASE_URL/query" \
      -H "Content-Type: application/json" \
      -d "{
        \"query\": \"$query\",
        \"userId\": \"$user_id\"
      }")
    
    # Display response
    echo "Response:"
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    echo ""
    echo "🔍 Check the application logs to see:"
    echo "  - RouterActor using 'tell' pattern to route to $expected_department"
    echo "  - ${expected_department}Actor using 'ask' pattern with LLMActor"
    if [ "$expected_department" = "pharmacy" ]; then
        echo "  - PharmacyActor using 'forward' pattern through LoggerActor"
    else
        echo "  - ${expected_department}Actor using 'tell' pattern to LoggerActor"
    fi
    echo ""
    read -p "Press Enter to continue..."
    echo ""
}

# Test 1: General Medicine Query (tell + ask patterns)
send_query "I have been experiencing chest pain and shortness of breath. Should I be worried?" "patient-001" "general-medicine"

# Test 2: Pharmacy Query (tell + ask + forward patterns)  
send_query "What is the recommended dosage for ibuprofen and can I take it with blood pressure medication?" "patient-002" "pharmacy"

# Test 3: Radiology Query (tell + ask patterns)
send_query "I need to get an MRI scan for my knee. What should I expect and how should I prepare?" "patient-003" "radiology"

# Test 4: Another General Medicine Query
send_query "What are the early symptoms of diabetes and when should I see a doctor?" "patient-004" "general-medicine"

# Test 5: Another Pharmacy Query (demonstrates forward pattern again)
send_query "Are there any side effects of taking aspirin daily for heart health?" "patient-005" "pharmacy"

# Test 6: Edge case - ambiguous query (should default to general medicine)
send_query "I'm feeling tired and have trouble sleeping. Any advice?" "patient-006" "general-medicine"

echo "=============================================="
echo "Interactive Testing Complete!"
echo ""
echo "Summary of Communication Patterns Demonstrated:"
echo ""
echo "🎯 TELL Pattern (Fire-and-forget):"
echo "   • RouterActor → DepartmentActors (routing queries)"
echo "   • DepartmentActors → LoggerActor (logging)"
echo ""
echo "🎯 ASK Pattern (Request-response):" 
echo "   • HTTP Server → RouterActor (user requests)"
echo "   • DepartmentActors → LLMActor (LLM processing)"
echo ""
echo "🎯 FORWARD Pattern (Maintain original sender):"
echo "   • PharmacyActor → LoggerActor → User"
echo "   • Logger handles logging AND forwards response"
echo ""
echo "Check the application logs to see detailed actor communication!"
echo "=============================================="
