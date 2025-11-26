# Postman Collection Guide

## Import the Collection

1. Open Postman
2. Click **Import**
3. Select `FX-Deal-Warehouse.postman_collection.json`
4. The collection will be imported with all 16 tests

## Configuration

The collection uses one variable:
- `baseUrl`: Default is `http://localhost:8080`

To change it:
1. Open the collection
2. Go to **Variables** tab
3. Update `baseUrl` value

## Running Tests

### Option 1: Run Individual Test
1. Open a request in the collection
2. Click **Send**
3. View results in **Test Results** tab

### Option 2: Run All Tests
1. Click the three dots next to the collection name
2. Select **Run collection**
3. Click **Run FX Deal Warehouse API**
4. View summary of all test results

### Option 3: Command Line (Newman)
```bash
# Install Newman (once)
npm install -g newman

# Run collection
newman run FX-Deal-Warehouse.postman_collection.json

# With HTML report
newman run FX-Deal-Warehouse.postman_collection.json -r htmlextra
```

## Quick Verification Script

For a quick API check, run:
```powershell
.\test-api-quick.ps1
```

This runs 9 essential tests in seconds.

## Test Coverage

The collection includes 16 tests covering:
- ✅ Health check
- ✅ Valid deal import (all currency pairs)
- ✅ Validation tests (missing fields, invalid data)
- ✅ Duplicate detection
- ✅ Bulk import (all valid, mixed valid/invalid)
- ✅ Edge cases (large/small amounts, different timestamps)

All tests use dynamic IDs to avoid conflicts on repeated runs.

## Troubleshooting

### Connection Refused
Make sure the application is running:
```bash
make up
```

### Tests Failing
1. Check application logs
2. Verify database is running
3. Ensure port 8080 is not in use by another process

### Duplicate Errors
Tests generate unique IDs automatically, but if you get duplicates:
1. Clean the database: restart with `make down && make up`
2. Or manually delete deals via the API

## Support

For more information, see the main README.md in the project root.

