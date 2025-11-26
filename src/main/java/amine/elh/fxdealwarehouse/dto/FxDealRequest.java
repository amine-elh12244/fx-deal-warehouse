package amine.elh.fxdealwarehouse.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxDealRequest {

    @NotBlank(message = "Deal unique ID is required")
    @Size(max = 100, message = "Deal unique ID must not exceed 100 characters")
    private String dealUniqueId;

    @NotBlank(message = "From currency ISO code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "From currency must be a valid 3-letter ISO code")
    private String fromCurrencyIsoCode;

    @NotBlank(message = "To currency ISO code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "To currency must be a valid 3-letter ISO code")
    private String toCurrencyIsoCode;

    @NotNull(message = "Deal timestamp is required")
    @PastOrPresent(message = "Deal timestamp cannot be in the future")
    private LocalDateTime dealTimestamp;

    @NotNull(message = "Deal amount is required")
    @DecimalMin(value = "0.0001", inclusive = true, message = "Deal amount must be positive")
    @Digits(integer = 15, fraction = 4, message = "Deal amount format is invalid")
    private BigDecimal dealAmount;
}