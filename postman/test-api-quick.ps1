$ErrorActionPreference = "SilentlyContinue"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  TEST RAPIDE API FX DEAL WAREHOUSE" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$timestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$testsPassed = 0
$testsFailed = 0

Write-Host "Base URL: $baseUrl" -ForegroundColor White
Write-Host "Timestamp: $timestamp`n" -ForegroundColor White

# Test 1: Health Check
Write-Host "[1/9] Health Check..." -NoNewline
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals/health" -Method Get -ErrorAction Stop
    if ($response -like "*running*") {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
} catch {
    Write-Host " FAIL - App not running" -ForegroundColor Red
    Write-Host "`nERROR: Cannot connect to $baseUrl" -ForegroundColor Red
    Write-Host "Please start the application first: make up`n" -ForegroundColor Yellow
    exit 1
}

# Test 2: Import valid deal
Write-Host "[2/9] Import valid deal..." -NoNewline
$dealId = "TEST-$timestamp"
$validDeal = @{
    dealUniqueId = $dealId
    fromCurrencyIsoCode = "USD"
    toCurrencyIsoCode = "EUR"
    dealTimestamp = "2024-01-25T10:30:00"
    dealAmount = 1000.50
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Post -Body $validDeal -ContentType "application/json" -ErrorAction Stop
    if ($response.dealUniqueId -eq $dealId) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
} catch {
    Write-Host " FAIL" -ForegroundColor Red
    $testsFailed++
}

# Test 3: Duplicate deal (should fail with 409)
Write-Host "[3/9] Reject duplicate..." -NoNewline
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Post -Body $validDeal -ContentType "application/json" -ErrorAction Stop
    Write-Host " FAIL - Duplicate accepted" -ForegroundColor Red
    $testsFailed++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
}

# Test 4: Missing field validation
Write-Host "[4/9] Validate missing field..." -NoNewline
$invalidDeal = @{
    fromCurrencyIsoCode = "USD"
    toCurrencyIsoCode = "EUR"
    dealTimestamp = "2024-01-25T10:30:00"
    dealAmount = 1000.50
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Post -Body $invalidDeal -ContentType "application/json" -ErrorAction Stop
    Write-Host " FAIL - Invalid accepted" -ForegroundColor Red
    $testsFailed++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
}

# Test 5: Invalid currency
Write-Host "[5/9] Reject invalid currency..." -NoNewline
$invalidCurrency = @{
    dealUniqueId = "TEST-INV-$timestamp"
    fromCurrencyIsoCode = "XXX"
    toCurrencyIsoCode = "EUR"
    dealTimestamp = "2024-01-25T10:30:00"
    dealAmount = 1000.50
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Post -Body $invalidCurrency -ContentType "application/json" -ErrorAction Stop
    Write-Host " FAIL - Invalid currency accepted" -ForegroundColor Red
    $testsFailed++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
}

# Test 6: Same currencies
Write-Host "[6/9] Reject same currencies..." -NoNewline
$sameCurrency = @{
    dealUniqueId = "TEST-SAME-$timestamp"
    fromCurrencyIsoCode = "USD"
    toCurrencyIsoCode = "USD"
    dealTimestamp = "2024-01-25T10:30:00"
    dealAmount = 1000.50
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Post -Body $sameCurrency -ContentType "application/json" -ErrorAction Stop
    Write-Host " FAIL - Same currencies accepted" -ForegroundColor Red
    $testsFailed++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
}

# Test 7: Negative amount
Write-Host "[7/9] Reject negative amount..." -NoNewline
$negativeAmount = @{
    dealUniqueId = "TEST-NEG-$timestamp"
    fromCurrencyIsoCode = "USD"
    toCurrencyIsoCode = "EUR"
    dealTimestamp = "2024-01-25T10:30:00"
    dealAmount = -100.00
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Post -Body $negativeAmount -ContentType "application/json" -ErrorAction Stop
    Write-Host " FAIL - Negative accepted" -ForegroundColor Red
    $testsFailed++
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
}

# Test 8: Bulk import
Write-Host "[8/9] Bulk import..." -NoNewline
$bulkDeals = @(
    @{
        dealUniqueId = "BULK-1-$timestamp"
        fromCurrencyIsoCode = "USD"
        toCurrencyIsoCode = "EUR"
        dealTimestamp = "2024-01-25T10:00:00"
        dealAmount = 1000.00
    },
    @{
        dealUniqueId = "BULK-2-$timestamp"
        fromCurrencyIsoCode = "GBP"
        toCurrencyIsoCode = "JPY"
        dealTimestamp = "2024-01-25T11:00:00"
        dealAmount = 2000.00
    }
) | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals/bulk" -Method Post -Body $bulkDeals -ContentType "application/json" -ErrorAction Stop
    if ($response.Count -eq 2) {
        Write-Host " PASS" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $testsFailed++
    }
} catch {
    Write-Host " FAIL" -ForegroundColor Red
    $testsFailed++
}

# Test 9: Get all deals
Write-Host "[9/9] Get all deals..." -NoNewline
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/deals" -Method Get -ErrorAction Stop
    if ($response.Count -gt 0) {
        Write-Host " PASS ($($response.Count) deals)" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host " WARN - Empty" -ForegroundColor Yellow
        $testsPassed++
    }
} catch {
    Write-Host " FAIL" -ForegroundColor Red
    $testsFailed++
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "             RESULTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Passed: " -NoNewline
Write-Host "$testsPassed/9" -ForegroundColor Green
Write-Host "Failed: " -NoNewline
if ($testsFailed -gt 0) {
    Write-Host "$testsFailed/9" -ForegroundColor Red
} else {
    Write-Host "$testsFailed/9" -ForegroundColor Green
}

if ($testsFailed -eq 0) {
    Write-Host "`nSTATUS: ALL TESTS PASSED!" -ForegroundColor Green -BackgroundColor Black
} else {
    Write-Host "`nSTATUS: SOME TESTS FAILED" -ForegroundColor Red -BackgroundColor Black
}

Write-Host "`nFor complete testing, use Postman:" -ForegroundColor White
Write-Host "  Import: FX-Deal-Warehouse.postman_collection.json" -ForegroundColor Cyan
Write-Host "  Run all 16 tests in Postman`n" -ForegroundColor Cyan

exit $testsFailed




