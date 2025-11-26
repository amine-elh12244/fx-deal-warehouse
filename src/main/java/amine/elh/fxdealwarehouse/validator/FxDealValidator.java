package amine.elh.fxdealwarehouse.validator;



import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.exception.InvalidDealException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class FxDealValidator {

    private static final Set<String> VALID_CURRENCY_CODES = new HashSet<>(Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD",
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "BGN",
            "TRY", "RUB", "INR", "CNY", "HKD", "SGD", "KRW", "THB",
            "MYR", "IDR", "PHP", "MXN", "BRL", "ARS", "CLP", "COP",
            "ZAR", "SAR", "AED", "ILS", "EGP", "NGN", "KES"
    ));

    public void validate(FxDealRequest request) {
        log.debug("Validating FX deal request: {}", request.getDealUniqueId());

        validateCurrencyCodes(request);
        validateCurrenciesAreDifferent(request);
    }

    private void validateCurrencyCodes(FxDealRequest request) {
        if (!VALID_CURRENCY_CODES.contains(request.getFromCurrencyIsoCode())) {
            throw new InvalidDealException(
                    "Invalid from currency code: " + request.getFromCurrencyIsoCode()
            );
        }

        if (!VALID_CURRENCY_CODES.contains(request.getToCurrencyIsoCode())) {
            throw new InvalidDealException(
                    "Invalid to currency code: " + request.getToCurrencyIsoCode()
            );
        }
    }

    private void validateCurrenciesAreDifferent(FxDealRequest request) {
        if (request.getFromCurrencyIsoCode().equals(request.getToCurrencyIsoCode())) {
            throw new InvalidDealException(
                    "From and to currencies must be different"
            );
        }
    }
}