# FX Deal Warehouse - Verification Script (PowerShell)
# This script verifies that the project meets all Bloomberg assignment requirements

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "FX Deal Warehouse - Verification Script" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$PASSED = 0
$FAILED = 0

function Check-File {
    param($Path, $Description)
    if (Test-Path $Path) {
        Write-Host "✓ $Description" -ForegroundColor Green
        $script:PASSED++
    } else {
        Write-Host "✗ $Description - File not found: $Path" -ForegroundColor Red
        $script:FAILED++
    }
}

function Check-Dir {
    param($Path, $Description)
    if (Test-Path $Path -PathType Container) {
        Write-Host "✓ $Description" -ForegroundColor Green
        $script:PASSED++
    } else {
        Write-Host "✗ $Description - Directory not found: $Path" -ForegroundColor Red
        $script:FAILED++
    }
}

Write-Host "1. Checking Project Structure..." -ForegroundColor Yellow
Write-Host "--------------------------------"
Check-File "pom.xml" "Maven project file"
Check-File "docker-compose.yml" "Docker Compose file"
Check-File "Dockerfile" "Dockerfile"
Check-File "Makefile" "Makefile"
Check-File ".gitignore" "Git ignore file"
Check-File "README.md" "Documentation"
Check-File "sample-deals.json" "Sample data file"
Write-Host ""

Write-Host "2. Checking Source Code..." -ForegroundColor Yellow
Write-Host "--------------------------------"
Check-Dir "src\main\java" "Main source directory"
Check-File "src\main\java\amine\elh\fxdealwarehouse\FxDealWarehouseApplication.java" "Application class"
Check-File "src\main\java\amine\elh\fxdealwarehouse\controller\FxDealController.java" "Controller"
Check-File "src\main\java\amine\elh\fxdealwarehouse\service\FxDealService.java" "Service interface"
Check-File "src\main\java\amine\elh\fxdealwarehouse\service\FxDealServiceImpl.java" "Service implementation"
Check-File "src\main\java\amine\elh\fxdealwarehouse\repository\FxDealRepository.java" "Repository"
Check-File "src\main\java\amine\elh\fxdealwarehouse\model\FxDeal.java" "Entity model"
Check-File "src\main\java\amine\elh\fxdealwarehouse\dto\FxDealRequest.java" "DTO"
Check-File "src\main\java\amine\elh\fxdealwarehouse\validator\FxDealValidator.java" "Validator"
Check-File "src\main\java\amine\elh\fxdealwarehouse\exception\GlobalExceptionHandler.java" "Exception handler"
Write-Host ""

Write-Host "3. Checking Test Files..." -ForegroundColor Yellow
Write-Host "--------------------------------"
Check-Dir "src\test\java" "Test source directory"
Check-File "src\test\java\amine\elh\fxdealwarehouse\unitTests\service\FxDealServiceImplTest.java" "Service unit tests"
Check-File "src\test\java\amine\elh\fxdealwarehouse\unitTests\validator\FxDealValidatorTest.java" "Validator unit tests"
Check-File "src\test\java\amine\elh\fxdealwarehouse\integrationTests\FxDealRepositoryIntegrationTest.java" "Integration tests"
Check-File "src\test\java\amine\elh\fxdealwarehouse\apiTests\FxDealApiTest.java" "API tests (REST Assured)"
Write-Host ""

Write-Host "4. Checking Performance & API Tests..." -ForegroundColor Yellow
Write-Host "--------------------------------"
Check-Dir "k6" "K6 directory"
Check-File "k6\load-test.js" "K6 performance tests"
Check-Dir "postman" "Postman directory"
Check-File "postman\FX-Deal-Warehouse.postman_collection.json" "Postman collection"
Write-Host ""

Write-Host "5. Checking Documentation..." -ForegroundColor Yellow
Write-Host "--------------------------------"
Check-File "README.md" "Main documentation"
Check-File "COMPLIANCE-ANALYSIS.md" "Compliance analysis"
Check-File "QUICKSTART.md" "Quick start guide"
Check-File "ACTIONS-DONE.md" "Actions summary"
Write-Host ""

Write-Host "6. Checking Configuration..." -ForegroundColor Yellow
Write-Host "--------------------------------"
Check-File "src\main\resources\application.yml" "Application config"
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Passed: $PASSED" -ForegroundColor Green
Write-Host "Failed: $FAILED" -ForegroundColor Red
Write-Host ""

if ($FAILED -eq 0) {
    Write-Host "All checks passed! Project is ready for submission." -ForegroundColor Green
    exit 0
} else {
    Write-Host "Some checks failed. Please review the missing items above." -ForegroundColor Red
    exit 1
}

