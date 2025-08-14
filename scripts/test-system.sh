#!/bin/bash

# Test script for the Health Assistant System

BASE_URL="http://localhost:8080"

echo "==================================="
echo "Health Assistant System Test Script"
echo "==================================="
echo ""

# Test 1: Health Check
echo "1. Testing Health Check..."
curl -s "$BASE_URL/health"
echo ""
echo ""

# Test 2: System Info
echo "2. Testing System Info..."
curl -s "$BASE_URL/info" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/info"
echo ""
echo ""

# Test 3: General Medicine Query (Symptom Checker)
echo "3. Testing Symptom Checker (General Medicine)..."
curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "I have a sore throat and fever. What could this be?",
    "userId": "test-user-001"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "I have a sore throat and fever. What could this be?",
    "userId": "test-user-001"
  }'
echo ""
echo ""

# Test 4: Pharmacy Query (Medication Lookup)
echo "4. Testing Medication Lookup (Pharmacy)..."
curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is ibuprofen used for and what are the side effects?",
    "userId": "test-user-002"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is ibuprofen used for and what are the side effects?",
    "userId": "test-user-002"
  }'
echo ""
echo ""

# Test 5: Radiology Query
echo "5. Testing Radiology Query..."
curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What should I expect during an MRI scan?",
    "userId": "test-user-003"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What should I expect during an MRI scan?",
    "userId": "test-user-003"
  }'
echo ""
echo ""

# Test 6: General Medical Query
echo "6. Testing General Medical Query..."
curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the symptoms of diabetes?",
    "userId": "test-user-004"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the symptoms of diabetes?",
    "userId": "test-user-004"
  }'
echo ""
echo ""

echo "==================================="
echo "Test completed!"
echo "Check the logs for actor communication patterns:"
echo "- tell: Router -> Department Actors"
echo "- ask: Department Actors -> LLM Actor"
echo "- forward: Pharmacy Actor -> Logger Actor"
echo "==================================="
