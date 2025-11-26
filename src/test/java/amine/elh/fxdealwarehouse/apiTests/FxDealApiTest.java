package amine.elh.fxdealwarehouse.apiTests;

import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.model.FxDeal;
import amine.elh.fxdealwarehouse.repository.FxDealRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FxDealApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private FxDealRepository repository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/deals";
        repository.deleteAll();
    }


    @Test
    @Order(1)
    @DisplayName("Should accept and store all valid fields correctly")
    void testValidFieldAcceptance() {
        FxDealRequest request = createValidRequest("DEAL-001");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("dealUniqueId", equalTo("DEAL-001"))
                .body("fromCurrencyIsoCode", equalTo("USD"))
                .body("toCurrencyIsoCode", equalTo("EUR"))
                .body("dealAmount", equalTo(1000.50f))
                .body("dealTimestamp", notNullValue())
                .body("id", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should validate ISO currency codes - valid codes")
    void testValidIsoCurrencyCodes() {
        String[] validCodes = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"};

        for (String code : validCodes) {
            FxDealRequest request = createValidRequest("DEAL-ISO-" + code);
            request.setFromCurrencyIsoCode(code);
            // Use different toCurrency to avoid same currency validation
            request.setToCurrencyIsoCode(code.equals("EUR") ? "USD" : "EUR");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post()
                    .then()
                    .statusCode(201)
                    .body("fromCurrencyIsoCode", equalTo(code));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should reject invalid ISO currency codes")
    void testInvalidIsoCurrencyCodes() {
        String[] invalidCodes = {"US", "EURO", "XXX", "123", ""};

        for (String code : invalidCodes) {
            FxDealRequest request = createValidRequest("DEAL-INVALID-" + code);
            request.setFromCurrencyIsoCode(code);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post()
                    .then()
                    .statusCode(400);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should reject null or empty deal unique ID")
    void testNullOrEmptyDealId() {
        FxDealRequest request = createValidRequest(null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);

        request.setDealUniqueId("");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("Should reject null or invalid deal amount")
    void testInvalidDealAmount() {
        // Null amount
        FxDealRequest request = createValidRequest("DEAL-NULL-AMOUNT");
        request.setDealAmount(null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);

        // Negative amount
        request = createValidRequest("DEAL-NEG-AMOUNT");
        request.setDealAmount(BigDecimal.valueOf(-100));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);

        // Zero amount
        request = createValidRequest("DEAL-ZERO-AMOUNT");
        request.setDealAmount(BigDecimal.ZERO);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("Should reject null or future deal timestamp")
    void testInvalidDealTimestamp() {
        // Null timestamp
        FxDealRequest request = createValidRequest("DEAL-NULL-TIME");
        request.setDealTimestamp(null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);

        // Future timestamp
        request = createValidRequest("DEAL-FUTURE-TIME");
        request.setDealTimestamp(LocalDateTime.now().plusDays(1));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    // ============================================================================
    // REQUIREMENT 2: Row-Level Validation Tests
    // Tests that validation happens at row level and invalid rows are rejected
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Should validate same currency pairs")
    void testSameCurrencyPair() {
        FxDealRequest request = createValidRequest("DEAL-SAME-CURR");
        request.setFromCurrencyIsoCode("USD");
        request.setToCurrencyIsoCode("USD");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .body("message", containsString("From and to currencies must be different"));
    }

    @Test
    @Order(8)
    @DisplayName("Should accept deal with valid high precision amount")
    void testHighPrecisionAmount() {
        FxDealRequest request = createValidRequest("DEAL-PRECISION");
        request.setDealAmount(new BigDecimal("1234567.89"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("dealAmount", equalTo(1234567.89f));
    }

    @Test
    @Order(9)
    @DisplayName("Should validate all fields individually")
    void testIndividualFieldValidation() {
        // Each field should be validated independently
        FxDealRequest request = createValidRequest("DEAL-MULTI-ERROR");
        request.setFromCurrencyIsoCode("INVALID");
        request.setDealAmount(BigDecimal.valueOf(-100));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    // ============================================================================
    // REQUIREMENT 3: Duplicate Prevention Tests
    // Tests that the same deal cannot be imported twice
    // ============================================================================

    @Test
    @Order(10)
    @DisplayName("Should prevent duplicate deal import with same ID")
    void testDuplicateDealPrevention() {
        FxDealRequest request = createValidRequest("DEAL-DUPLICATE");

        // First import - should succeed
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201);

        // Second import with same ID - should fail
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(409)
                .body("message", containsString("already exists"));
    }

    @Test
    @Order(11)
    @DisplayName("Should allow deals with same details but different IDs")
    void testDifferentIdsWithSameDetails() {
        FxDealRequest request1 = createValidRequest("DEAL-A");
        FxDealRequest request2 = createValidRequest("DEAL-B");
        // Same details, different IDs
        request2.setFromCurrencyIsoCode(request1.getFromCurrencyIsoCode());
        request2.setToCurrencyIsoCode(request1.getToCurrencyIsoCode());
        request2.setDealAmount(request1.getDealAmount());

        given()
                .contentType(ContentType.JSON)
                .body(request1)
                .when()
                .post()
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(request2)
                .when()
                .post()
                .then()
                .statusCode(201);
    }

    @Test
    @Order(12)
    @DisplayName("Should detect duplicate in bulk operation")
    void testDuplicateInBulkOperation() {
        FxDealRequest deal1 = createValidRequest("DEAL-BULK-1");
        FxDealRequest deal2 = createValidRequest("DEAL-BULK-2");
        FxDealRequest deal3 = createValidRequest("DEAL-BULK-1"); // Duplicate of deal1

        List<FxDealRequest> requests = Arrays.asList(deal1, deal2, deal3);

        given()
                .contentType(ContentType.JSON)
                .body(requests)
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(2)); // Only 2 should be imported
    }

    // ============================================================================
    // REQUIREMENT 4: Partial Success with No Rollback Tests
    // Tests that bulk operations continue despite failures and don't rollback
    // ============================================================================

    @Test
    @Order(13)
    @DisplayName("Should import valid deals despite invalid ones in bulk")
    void testPartialSuccessInBulk() {
        FxDealRequest valid1 = createValidRequest("DEAL-VALID-1");
        FxDealRequest invalid = createValidRequest("DEAL-INVALID");
        invalid.setFromCurrencyIsoCode("INVALID_CODE");
        FxDealRequest valid2 = createValidRequest("DEAL-VALID-2");

        List<FxDealRequest> requests = Arrays.asList(valid1, invalid, valid2);

        given()
                .contentType(ContentType.JSON)
                .body(requests)
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(2))
                .body("dealUniqueId", hasItems("DEAL-VALID-1", "DEAL-VALID-2"));

        // Verify both valid deals are persisted
        List<FxDeal> allDeals = repository.findAll();
        Assertions.assertEquals(2, allDeals.size());
    }

    @Test
    @Order(14)
    @DisplayName("Should continue processing after duplicate in bulk")
    void testContinueAfterDuplicateInBulk() {
        // Pre-import one deal
        FxDealRequest preImported = createValidRequest("DEAL-PRE");
        repository.save(mapToDeal(preImported));

        FxDealRequest valid1 = createValidRequest("DEAL-POST-1");
        FxDealRequest duplicate = createValidRequest("DEAL-PRE"); // Duplicate
        FxDealRequest valid2 = createValidRequest("DEAL-POST-2");

        List<FxDealRequest> requests = Arrays.asList(valid1, duplicate, valid2);

        given()
                .contentType(ContentType.JSON)
                .body(requests)
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(2));

        // Verify new deals are persisted (total 3: 1 pre-imported + 2 new)
        List<FxDeal> allDeals = repository.findAll();
        Assertions.assertEquals(3, allDeals.size());
    }

    @Test
    @Order(15)
    @DisplayName("Should not rollback on validation failure in bulk")
    void testNoRollbackOnValidationFailure() {
        FxDealRequest valid1 = createValidRequest("DEAL-NO-ROLLBACK-1");
        FxDealRequest valid2 = createValidRequest("DEAL-NO-ROLLBACK-2");
        FxDealRequest invalid1 = createValidRequest("DEAL-INVALID-CURR");
        invalid1.setFromCurrencyIsoCode("USD");
        invalid1.setToCurrencyIsoCode("USD"); // Same currency
        FxDealRequest valid3 = createValidRequest("DEAL-NO-ROLLBACK-3");
        FxDealRequest invalid2 = createValidRequest("DEAL-INVALID-AMT");
        invalid2.setDealAmount(BigDecimal.valueOf(-500));
        FxDealRequest valid4 = createValidRequest("DEAL-NO-ROLLBACK-4");

        List<FxDealRequest> requests = Arrays.asList(
                valid1, valid2, invalid1, valid3, invalid2, valid4
        );

        given()
                .contentType(ContentType.JSON)
                .body(requests)
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(4));

        // Verify all 4 valid deals are persisted
        List<FxDeal> allDeals = repository.findAll();
        Assertions.assertEquals(4, allDeals.size());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle bulk with all invalid deals gracefully")
    void testAllInvalidDealsInBulk() {
        FxDealRequest invalid1 = createValidRequest("DEAL-ALL-INVALID-1");
        invalid1.setDealAmount(BigDecimal.valueOf(-100));

        FxDealRequest invalid2 = createValidRequest("DEAL-ALL-INVALID-2");
        invalid2.setFromCurrencyIsoCode("INVALID");

        FxDealRequest invalid3 = createValidRequest("DEAL-ALL-INVALID-3");
        invalid3.setDealTimestamp(LocalDateTime.now().plusDays(1));

        List<FxDealRequest> requests = Arrays.asList(invalid1, invalid2, invalid3);

        given()
                .contentType(ContentType.JSON)
                .body(requests)
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(0));

        // Verify no deals are persisted
        List<FxDeal> allDeals = repository.findAll();
        Assertions.assertEquals(0, allDeals.size());
    }

    // ============================================================================
    // EDGE CASES & ADDITIONAL TESTS
    // ============================================================================

    @Test
    @Order(17)
    @DisplayName("Should handle empty bulk request")
    void testEmptyBulkRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList())
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(18)
    @DisplayName("Should handle large bulk import")
    void testLargeBulkImport() {
        List<FxDealRequest> requests = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            requests.add(createValidRequest("DEAL-LARGE-" + i));
        }

        given()
                .contentType(ContentType.JSON)
                .body(requests)
                .when()
                .post("/bulk")
                .then()
                .statusCode(201)
                .body("size()", equalTo(100));
    }

    @Test
    @Order(19)
    @DisplayName("Should retrieve all imported deals")
    void testGetAllDeals() {
        // Import some deals
        repository.save(mapToDeal(createValidRequest("DEAL-GET-1")));
        repository.save(mapToDeal(createValidRequest("DEAL-GET-2")));
        repository.save(mapToDeal(createValidRequest("DEAL-GET-3")));

        given()
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("size()", equalTo(3))
                .body("dealUniqueId", hasItems("DEAL-GET-1", "DEAL-GET-2", "DEAL-GET-3"));
    }

    @Test
    @Order(20)
    @DisplayName("Should verify health endpoint")
    void testHealthEndpoint() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body(containsString("running"));
    }

    @Test
    @Order(21)
    @DisplayName("Should handle deal with very large amount")
    void testVeryLargeAmount() {
        FxDealRequest request = createValidRequest("DEAL-LARGE-AMT");
        request.setDealAmount(new BigDecimal("999999999.99"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("dealAmount", notNullValue());
    }

    @Test
    @Order(22)
    @DisplayName("Should handle deal with timestamp at boundary")
    void testTimestampBoundary() {
        FxDealRequest request = createValidRequest("DEAL-TIME-BOUND");
        request.setDealTimestamp(LocalDateTime.now().minusSeconds(1));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private FxDealRequest createValidRequest(String dealId) {
        return FxDealRequest.builder()
                .dealUniqueId(dealId)
                .fromCurrencyIsoCode("USD")
                .toCurrencyIsoCode("EUR")
                .dealTimestamp(LocalDateTime.now().minusHours(1))
                .dealAmount(BigDecimal.valueOf(1000.50))
                .build();
    }

    private FxDeal mapToDeal(FxDealRequest request) {
        return FxDeal.builder()
                .dealUniqueId(request.getDealUniqueId())
                .fromCurrencyIsoCode(request.getFromCurrencyIsoCode())
                .toCurrencyIsoCode(request.getToCurrencyIsoCode())
                .dealTimestamp(request.getDealTimestamp())
                .dealAmount(request.getDealAmount())
                .build();
    }
}
