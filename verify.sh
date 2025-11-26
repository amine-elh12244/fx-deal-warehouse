#!/bin/bash

# FX Deal Warehouse - Verification Script
# This script verifies that the project meets all Bloomberg assignment requirements

echo "=========================================="
echo "FX Deal Warehouse - Verification Script"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 - File not found: $1"
        ((FAILED++))
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 - Directory not found: $1"
        ((FAILED++))
    fi
}

echo "1. Checking Project Structure..."
echo "--------------------------------"
check_file "pom.xml" "Maven project file"
check_file "docker-compose.yml" "Docker Compose file"
check_file "Dockerfile" "Dockerfile"
check_file "Makefile" "Makefile"
check_file ".gitignore" "Git ignore file"
check_file "README.md" "Documentation"
check_file "sample-deals.json" "Sample data file"
echo ""

echo "2. Checking Source Code..."
echo "--------------------------------"
check_dir "src/main/java" "Main source directory"
check_file "src/main/java/amine/elh/fxdealwarehouse/FxDealWarehouseApplication.java" "Application class"
check_file "src/main/java/amine/elh/fxdealwarehouse/controller/FxDealController.java" "Controller"
check_file "src/main/java/amine/elh/fxdealwarehouse/service/FxDealService.java" "Service interface"
check_file "src/main/java/amine/elh/fxdealwarehouse/service/FxDealServiceImpl.java" "Service implementation"
check_file "src/main/java/amine/elh/fxdealwarehouse/repository/FxDealRepository.java" "Repository"
check_file "src/main/java/amine/elh/fxdealwarehouse/model/FxDeal.java" "Entity model"
check_file "src/main/java/amine/elh/fxdealwarehouse/dto/FxDealRequest.java" "DTO"
check_file "src/main/java/amine/elh/fxdealwarehouse/validator/FxDealValidator.java" "Validator"
check_file "src/main/java/amine/elh/fxdealwarehouse/exception/GlobalExceptionHandler.java" "Exception handler"
echo ""

echo "3. Checking Test Files..."
echo "--------------------------------"
check_dir "src/test/java" "Test source directory"
check_file "src/test/java/amine/elh/fxdealwarehouse/unitTests/service/FxDealServiceImplTest.java" "Service unit tests"
check_file "src/test/java/amine/elh/fxdealwarehouse/unitTests/validator/FxDealValidatorTest.java" "Validator unit tests"
check_file "src/test/java/amine/elh/fxdealwarehouse/integrationTests/FxDealRepositoryIntegrationTest.java" "Integration tests"
check_file "src/test/java/amine/elh/fxdealwarehouse/apiTests/FxDealApiTest.java" "API tests (REST Assured)"
echo ""

echo "4. Checking Performance & API Tests..."
echo "--------------------------------"
check_dir "k6" "K6 directory"
check_file "k6/load-test.js" "K6 performance tests"
check_dir "postman" "Postman directory"
check_file "postman/FX-Deal-Warehouse.postman_collection.json" "Postman collection"
echo ""

echo "5. Checking Documentation..."
echo "--------------------------------"
check_file "README.md" "Main documentation"
check_file "COMPLIANCE-ANALYSIS.md" "Compliance analysis"
check_file "QUICKSTART.md" "Quick start guide"
check_file "ACTIONS-DONE.md" "Actions summary"
echo ""

echo "6. Checking Configuration..."
echo "--------------------------------"
check_file "src/main/resources/application.yml" "Application config"
echo ""

echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Project is ready for submission.${NC}"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please review the missing items above.${NC}"
    exit 1
fi

