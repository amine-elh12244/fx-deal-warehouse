# FX Deal Warehouse - Bloomberg Data Warehouse

## 📋 Project Overview

A Spring Boot application for Bloomberg to import, validate, and persist FX (Foreign Exchange) deal details into a PostgreSQL database. This system handles deal ingestion with comprehensive validation, duplicate prevention, and partial success semantics (no rollback).

---

## 🏗️ Technical Stack

- **Backend**: Spring Boot 3.4.12 (Java 17)
- **Database**: PostgreSQL 15
- **Build Tool**: Maven 3.9+
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito, REST Assured, TestContainers
- **Performance Testing**: K6
- **Code Coverage**: JaCoCo (100% target for business logic)

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Java 17+ (if running locally)
- Make (optional, for convenience commands)

### 1. Start the Application
```bash
make up
# or
docker-compose up --build -d
```

The application will be available at `http://localhost:8080`

### 2. Run Tests
```bash
make test
# or
./mvnw clean test
```

View coverage report: `target/site/jacoco/index.html`

### 3. Run Performance Tests
```bash
make k6-test
# or
k6 run k6/load-test.js
```

### 4. Test API with Postman
Import the collection: `postman/FX-Deal-Warehouse-v2.postman_collection.json`

---

## 📁 Project Structure

```
fx-deal-warehouse/
├── src/
│   ├── main/java/amine/elh/fxdealwarehouse/
│   │   ├── controller/       # REST API Controllers
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── exception/        # Exception Handling
│   │   ├── model/            # JPA Entities
│   │   ├── repository/       # Data Access Layer
│   │   ├── service/          # Business Logic
│   │   └── validator/        # Business Validation
│   └── test/java/amine/elh/fxdealwarehouse/
│       ├── apiTests/         # REST Assured E2E Tests
│       ├── integrationTests/ # Repository Integration Tests
│       └── unitTests/        # Unit Tests (Service, Validator)
├── k6/                       # K6 Performance Tests
├── postman/                  # Postman Collection & Documentation
├── docker-compose.yml        # Docker Compose Configuration
├── Dockerfile                # Application Docker Image
├── Makefile                  # Convenience Commands
└── pom.xml                   # Maven Configuration
```

---

## 🎯 Features & Requirements Compliance

### ✅ Requirement 1: Accept & Persist Deal Details

**Fields**:
- `dealUniqueId` (String, required, max 100 chars)
- `fromCurrencyIsoCode` (String, required, 3-letter ISO code)
- `toCurrencyIsoCode` (String, required, 3-letter ISO code)
- `dealTimestamp` (LocalDateTime, required, past or present)
- `dealAmount` (BigDecimal, required, positive, max 15 integer + 4 decimal digits)

### ✅ Requirement 2: Validate Row Structure

**Validation Rules**:
- **Missing fields**: All fields are mandatory (@NotNull, @NotBlank)
- **Type format**: 
  - Currency codes: 3 uppercase letters matching ISO 4217
  - Amount: Positive decimal with max 15 integer and 4 decimal digits
  - Timestamp: Past or present (@PastOrPresent)
- **Business rules**:
  - Currency codes must be valid (exist in Currency.getAvailableCurrencies())
  - From and To currencies must be different
  - Amount must be ≥ 0.0001

**Error Responses**:
- `400 Bad Request`: Validation failure with detailed field errors
- `409 Conflict`: Duplicate deal detected

### ✅ Requirement 3: No Duplicate Imports

**Implementation**:
- Database unique constraint on `dealUniqueId`
- Pre-insert check in service layer
- Returns `409 Conflict` if duplicate detected

### ✅ Requirement 4: No Rollback (Partial Success)

**Bulk Import Behavior**:
- Each valid deal is imported independently
- Invalid deals are skipped with logged errors
- Returns only successfully imported deals
- No transaction rollback on partial failures

---

## 🔌 API Endpoints

### 1. Import Single Deal
```http
POST /api/v1/deals
Content-Type: application/json

{
  "dealUniqueId": "DEAL-001",
  "fromCurrencyIsoCode": "USD",
  "toCurrencyIsoCode": "EUR",
  "dealTimestamp": "2024-01-25T10:30:00",
  "dealAmount": 1000.50
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "dealUniqueId": "DEAL-001",
  "fromCurrencyIsoCode": "USD",
  "toCurrencyIsoCode": "EUR",
  "dealTimestamp": "2024-01-25T10:30:00",
  "dealAmount": 1000.50,
  "importedAt": "2025-11-26T10:00:00"
}
```

### 2. Bulk Import
```http
POST /api/v1/deals/bulk
Content-Type: application/json

[
  { "dealUniqueId": "DEAL-002", ... },
  { "dealUniqueId": "DEAL-003", ... }
]
```

**Response** (201 Created): Array of successfully imported deals

### 3. Get All Deals
```http
GET /api/v1/deals
```

### 4. Health Check
```http
GET /api/v1/deals/health
```

---

## 🧪 Testing Strategy

### Test Coverage: 100% Target for Business Logic

JaCoCo enforces 100% coverage for:
- **Validation logic** (`FxDealValidator`)
- **Service layer** (`FxDealServiceImpl`)
- **Parsing and mapping** (DTO → Entity)
- **Deduplication logic**
- **Import flow**

### Excluded from Coverage (Justified)

| Component | Reason |
|-----------|--------|
| `FxDeal` entity | Lombok-generated code (getters/setters/builders) |
| `FxDealRequest` DTO | Lombok-generated + validation annotations only |
| `FxDealWarehouseApplication` | Spring Boot bootstrap class |
| Exception classes | Simple data classes with no business logic |
| Controllers | Tested via REST Assured (E2E tests) |

### Test Suites

#### 1. Unit Tests (27 tests)
**Location**: `src/test/java/.../unitTests/`

- **FxDealValidatorTest** (18 tests):
  - Valid/invalid currency codes
  - Same currency validation
  - Currency existence validation
  - Amount validation (negative, zero, positive)
  - Missing fields

- **FxDealServiceImplTest** (14 tests):
  - Import single deal (success/failure)
  - Duplicate detection
  - Bulk import (all valid, partial success, all invalid)
  - Edge cases

**Run**: `./mvnw test -Dtest=*UnitTest`

#### 2. Integration Tests (3 tests)
**Location**: `src/test/java/.../integrationTests/`

- **FxDealRepositoryIntegrationTest**:
  - Database constraints validation
  - Unique index enforcement
  - CRUD operations with real PostgreSQL (TestContainers)

**Run**: `./mvnw test -Dtest=*IntegrationTest`

#### 3. API Tests with REST Assured (27 tests)
**Location**: `src/test/java/.../apiTests/`

- **FxDealApiTest** - Comprehensive E2E tests covering:

| Category | Tests | Description |
|----------|-------|-------------|
| **Field Acceptance** | 9 tests | All required fields, all currency pairs, edge amounts |
| **Validation** | 8 tests | Missing fields, invalid formats, business rules |
| **Duplicate Prevention** | 3 tests | Same deal twice, duplicate in batch |
| **Partial Success** | 4 tests | Mixed valid/invalid, no rollback behavior |
| **Edge Cases** | 3 tests | Empty lists, extreme values, timestamps |

**Run**: `./mvnw test -Dtest=FxDealApiTest`

### Requirements → Tests Mapping

| Requirement | Test Coverage |
|-------------|---------------|
| **Accept all fields** | `FxDealApiTest`: tests 1-9 validate all fields and formats |
| **Validate structure** | `FxDealValidatorTest`: 18 validation tests<br>`FxDealApiTest`: tests 10-18 for API validation |
| **No duplicates** | `FxDealServiceImplTest.importDeal_DuplicateThrowsException`<br>`FxDealApiTest.importDeal_WithDuplicateId_ReturnsConflict`<br>`FxDealRepositoryIntegrationTest.saveDeal_WithDuplicateId_ThrowsException` |
| **No rollback** | `FxDealServiceImplTest.importDeals_PartialSuccess`<br>`FxDealApiTest.importDeals_WithMixedValidAndInvalid_ImportsOnlyValid`<br>`FxDealApiTest.verifyNoRollback_SuccessfulImportsRemainAfterFailures` |

---

## 📊 Running Tests & Viewing Coverage

### All Tests with Coverage
```bash
./mvnw clean test
```

**View Report**:
```bash
# Windows
start target/site/jacoco/index.html

# Linux/Mac
open target/site/jacoco/index.html
```

### Quick API Verification
```bash
cd postman
./test-api-quick.ps1
```

This runs 9 essential tests in seconds to verify the API is working.

---

## 🔥 Performance Testing with K6

### Run Load Test
```bash
k6 run k6/load-test.js
```

### Test Scenarios
- **Smoke Test**: 1 virtual user, 30 seconds
- **Load Test**: Ramp up to 50 users, 2 minutes
- **Stress Test**: Ramp up to 100 users, 3 minutes
- **Spike Test**: Sudden spike to 200 users

### Performance Thresholds
- 95% of requests < 500ms
- 99% of requests < 1000ms
- Error rate < 1%

---

## 🐳 Docker Deployment

### docker-compose.yml
Includes:
- PostgreSQL 15 database
- Spring Boot application
- Health checks and automatic restarts
- Persistent volume for database

### Environment Variables
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/fxdealwarehouse
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
```

---

## 📦 Makefile Commands

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make up` | Start all services (Docker) |
| `make test` | Run all tests with coverage |
| `make coverage` | Open coverage report in browser |
| `make k6-test` | Run K6 performance tests |
| `make build` | Build application (Maven) |
| `make clean` | Clean build artifacts |

---

## 📝 Logging

### Configuration
- **Level**: INFO for production, DEBUG for development
- **Format**: Timestamp, Level, Thread, Logger, Message
- **Output**: Console + Rolling file (`logs/fx-deal-warehouse.log`)
- **Retention**: 30 days, max 10 files of 10MB each

### Key Log Events
- Deal import attempts (with deal ID)
- Validation failures (with reasons)
- Duplicate detection
- Bulk import summary (success/failure counts)
- Exception stack traces

---

## 🛡️ Error Handling

### Global Exception Handler
- `DuplicateDealException` → 409 Conflict
- `InvalidDealException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 Bad Request (with field details)
- Generic exceptions → 500 Internal Server Error

### Error Response Format
```json
{
  "timestamp": "2025-11-26T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "validationErrors": {
    "dealUniqueId": "Deal unique ID is required",
    "dealAmount": "Deal amount must be positive"
  }
}
```

---

## 📚 Additional Documentation

- **Postman Collection**: `postman/FX-Deal-Warehouse.postman_collection.json`
- **Postman Guide**: `postman/README.md`
- **API Quick Tests**: `postman/test-api-quick.ps1`

---

## 🚦 Build & Deployment Status

### Requirements Compliance: ✅ 100%

| Requirement | Status | Evidence |
|-------------|--------|----------|
| PostgreSQL Database | ✅ | `docker-compose.yml`, `application.yml` |
| Docker Compose | ✅ | `docker-compose.yml` with sample data |
| Maven Project | ✅ | `pom.xml` with all dependencies |
| Error/Exception Handling | ✅ | `GlobalExceptionHandler`, custom exceptions |
| Logging | ✅ | SLF4J/Logback with file rotation |
| Unit Tests | ✅ | 27 unit tests, 100% coverage target |
| Integration Tests | ✅ | 3 tests with TestContainers |
| API Tests (REST Assured) | ✅ | 27 comprehensive E2E tests |
| JaCoCo Coverage | ✅ | 100% for business logic, build gate enforced |
| K6 Performance Tests | ✅ | `k6/load-test.js` with thresholds |
| Postman Collection | ✅ | 16 tests in Postman collection |
| GitHub Repository | ✅ | Ready for push |
| Makefile | ✅ | All required commands |

---

## 👨‍💻 Development

### Local Development
```bash
# Start PostgreSQL only
docker-compose up -d db

# Run application locally
./mvnw spring-boot:run

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing During Development
```bash
# Run tests on save
./mvnw test -Dtest=FxDealApiTest -Dspring-boot.run.fork=false

# Run specific test
./mvnw test -Dtest=FxDealValidatorTest#validate_WithInvalidCurrency_ThrowsException
```

---

## 📄 License

This project is developed as part of a technical assessment for Bloomberg.

---

## 📧 Contact

For questions or clarifications about this implementation, please refer to the test documentation or contact the development team.

---

**Last Updated**: November 26, 2025  
**Version**: 1.0.0  
**Status**: Production Ready ✅

