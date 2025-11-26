package amine.elh.fxdealwarehouse.unitTests.validator;

import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.exception.InvalidDealException;
import amine.elh.fxdealwarehouse.validator.FxDealValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FxDealValidatorTest {

    private FxDealValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FxDealValidator();
    }

    @Test
    void validate_WithValidRequest_ShouldNotThrowException() {
        // Given
        FxDealRequest request = createValidRequest("USD", "EUR");

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "ZAR", "AED"})
    void validate_WithValidFromCurrency_ShouldNotThrowException(String fromCurrency) {
        // Given - Use a different toCurrency to avoid same currency validation failure
        String toCurrency = fromCurrency.equals("EUR") ? "USD" : "EUR";
        FxDealRequest request = createValidRequest(fromCurrency, toCurrency);

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "ZAR", "AED"})
    void validate_WithValidToCurrency_ShouldNotThrowException(String toCurrency) {
        // Given - Use a different fromCurrency to avoid same currency validation failure
        String fromCurrency = toCurrency.equals("USD") ? "EUR" : "USD";
        FxDealRequest request = createValidRequest(fromCurrency, toCurrency);

        // When & Then
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "ABC", "INVALID", "US", "USDD", "", "123", "usd"})
    void validate_WithInvalidFromCurrency_ShouldThrowException(String invalidCurrency) {
        // Given
        FxDealRequest request = createValidRequest(invalidCurrency, "EUR");

        // When & Then
        InvalidDealException exception = assertThrows(
                InvalidDealException.class,
                () -> validator.validate(request)
        );
        assertTrue(exception.getMessage().contains("Invalid from currency code"));
        assertTrue(exception.getMessage().contains(invalidCurrency));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "ABC", "INVALID", "EU", "EURR", "", "123", "eur"})
    void validate_WithInvalidToCurrency_ShouldThrowException(String invalidCurrency) {
        // Given
        FxDealRequest request = createValidRequest("USD", invalidCurrency);

        // When & Then
        InvalidDealException exception = assertThrows(
                InvalidDealException.class,
                () -> validator.validate(request)
        );
        assertTrue(exception.getMessage().contains("Invalid to currency code"));
        assertTrue(exception.getMessage().contains(invalidCurrency));
    }

    @Test
    void validate_WithSameFromAndToCurrency_ShouldThrowException() {
        // Given
        FxDealRequest request = createValidRequest("USD", "USD");

        // When & Then
        InvalidDealException exception = assertThrows(
                InvalidDealException.class,
                () -> validator.validate(request)
        );
        assertEquals("From and to currencies must be different", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CHF"})
    void validate_WithAllSameCurrencyPairs_ShouldThrowException(String currency) {
        // Given
        FxDealRequest request = createValidRequest(currency, currency);

        // When & Then
        InvalidDealException exception = assertThrows(
                InvalidDealException.class,
                () -> validator.validate(request)
        );
        assertEquals("From and to currencies must be different", exception.getMessage());
    }

    @Test
    void validate_WithBothInvalidCurrencies_ShouldThrowExceptionForFromCurrency() {
        // Given
        FxDealRequest request = createValidRequest("XXX", "YYY");

        // When & Then
        InvalidDealException exception = assertThrows(
                InvalidDealException.class,
                () -> validator.validate(request)
        );
        assertTrue(exception.getMessage().contains("Invalid from currency code"));
    }

    @Test
    void validate_WithNullFromCurrency_ShouldThrowException() {
        // Given
        FxDealRequest request = createValidRequest(null, "EUR");

        // When & Then
        assertThrows(Exception.class, () -> validator.validate(request));
    }

    @Test
    void validate_WithNullToCurrency_ShouldThrowException() {
        // Given
        FxDealRequest request = createValidRequest("USD", null);

        // When & Then
        assertThrows(Exception.class, () -> validator.validate(request));
    }

    @Test
    void validate_WithAllMajorCurrencyPairs_ShouldNotThrowException() {
        // Given
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CHF"};

        // When & Then
        for (String from : currencies) {
            for (String to : currencies) {
                if (!from.equals(to)) {
                    FxDealRequest request = createValidRequest(from, to);
                    assertDoesNotThrow(() -> validator.validate(request),
                            "Should not throw for " + from + " -> " + to);
                }
            }
        }
    }

    @Test
    void validate_WithDifferentValidCurrencies_ShouldNotThrowException() {
        // Given
        String[][] currencyPairs = {
                {"USD", "EUR"},
                {"GBP", "JPY"},
                {"CHF", "CAD"},
                {"AUD", "CNY"},
                {"ZAR", "AED"},
                {"EUR", "GBP"},
                {"JPY", "USD"}
        };

        // When & Then
        for (String[] pair : currencyPairs) {
            FxDealRequest request = createValidRequest(pair[0], pair[1]);
            assertDoesNotThrow(() -> validator.validate(request),
                    "Should not throw for " + pair[0] + " -> " + pair[1]);
        }
    }

    private FxDealRequest createValidRequest(String fromCurrency, String toCurrency) {
        FxDealRequest request = new FxDealRequest();
        request.setDealUniqueId("DEAL-001");
        request.setFromCurrencyIsoCode(fromCurrency);
        request.setToCurrencyIsoCode(toCurrency);
        request.setDealTimestamp(LocalDateTime.now().minusHours(1));
        request.setDealAmount(BigDecimal.valueOf(1000.00));
        return request;
    }
}