package amine.elh.fxdealwarehouse.unitTests.service;

import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.exception.DuplicateDealException;
import amine.elh.fxdealwarehouse.exception.InvalidDealException;
import amine.elh.fxdealwarehouse.model.FxDeal;
import amine.elh.fxdealwarehouse.repository.FxDealRepository;
import amine.elh.fxdealwarehouse.service.FxDealServiceImpl;
import amine.elh.fxdealwarehouse.validator.FxDealValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxDealServiceImplTest {

    @Mock
    private FxDealRepository repository;

    @Mock
    private FxDealValidator validator;

    @Mock
    private Validator beanValidator; // Add the missing mock

    @InjectMocks
    private FxDealServiceImpl service;

    private FxDealRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = FxDealRequest.builder()
                .dealUniqueId("DEAL-001")
                .fromCurrencyIsoCode("USD")
                .toCurrencyIsoCode("EUR")
                .dealTimestamp(LocalDateTime.now().minusHours(1))
                .dealAmount(new BigDecimal("1000.50"))
                .build();
    }

    @Test
    void importDeal_Success() {
        // Given
        when(repository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });
        doNothing().when(validator).validate(any());

        // When
        FxDeal result = service.importDeal(validRequest);

        // Then
        assertNotNull(result);
        assertEquals("DEAL-001", result.getDealUniqueId());
        assertEquals("USD", result.getFromCurrencyIsoCode());
        verify(repository).save(any(FxDeal.class));
    }

    @Test
    void importDeal_DuplicateThrowsException() {
        // Given
        when(repository.existsByDealUniqueId("DEAL-001")).thenReturn(true);
        doNothing().when(validator).validate(any());

        // When & Then
        assertThrows(DuplicateDealException.class, () ->
                service.importDeal(validRequest)
        );
        verify(repository, never()).save(any());
    }

    @Test
    void importDeal_InvalidDealThrowsException() {
        // Given
        doThrow(new InvalidDealException("Invalid currency"))
                .when(validator).validate(any());

        // When & Then
        assertThrows(InvalidDealException.class, () ->
                service.importDeal(validRequest)
        );
        verify(repository, never()).save(any());
    }

    @Test
    void importDeals_BulkImport() {
        // Given
        FxDealRequest req1 = createRequest("DEAL-001");
        FxDealRequest req2 = createRequest("DEAL-002");
        List<FxDealRequest> requests = Arrays.asList(req1, req2);

        // Mock bean validator to return no violations
        when(beanValidator.validate(any(FxDealRequest.class)))
                .thenReturn(Collections.emptySet());

        when(repository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });
        doNothing().when(validator).validate(any());

        // When
        List<FxDeal> results = service.importDeals(requests);

        // Then
        assertEquals(2, results.size());
        verify(repository, times(2)).save(any(FxDeal.class));
        verify(beanValidator, times(2)).validate(any(FxDealRequest.class));
    }

    @Test
    void importDeals_PartialSuccess() {
        // Given
        FxDealRequest validReq = createRequest("DEAL-001");
        FxDealRequest duplicateReq = createRequest("DEAL-002");
        List<FxDealRequest> requests = Arrays.asList(validReq, duplicateReq);

        // Mock bean validator to return no violations
        when(beanValidator.validate(any(FxDealRequest.class)))
                .thenReturn(Collections.emptySet());

        when(repository.existsByDealUniqueId("DEAL-001")).thenReturn(false);
        when(repository.existsByDealUniqueId("DEAL-002")).thenReturn(true);
        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });
        doNothing().when(validator).validate(any());

        // When
        List<FxDeal> results = service.importDeals(requests);

        // Then
        assertEquals(1, results.size());
        verify(repository, times(1)).save(any(FxDeal.class));
    }

    @Test
    void importDeals_WithBeanValidationFailure_SkipsDeal() {
        // Given
        FxDealRequest invalidReq = createRequest("DEAL-001");
        FxDealRequest validReq = createRequest("DEAL-002");
        List<FxDealRequest> requests = Arrays.asList(invalidReq, validReq);

        // Mock bean validator to return violations for first request
        @SuppressWarnings("unchecked")
        ConstraintViolation<FxDealRequest> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Invalid field");

        when(beanValidator.validate(invalidReq)).thenReturn(Set.of(violation));
        when(beanValidator.validate(validReq)).thenReturn(Collections.emptySet());

        when(repository.existsByDealUniqueId("DEAL-002")).thenReturn(false);
        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });
        doNothing().when(validator).validate(any());

        // When
        List<FxDeal> results = service.importDeals(requests);

        // Then
        assertEquals(1, results.size());
        assertEquals("DEAL-002", results.get(0).getDealUniqueId());
        verify(repository, times(1)).save(any(FxDeal.class));
    }

    private FxDealRequest createRequest(String dealId) {
        return FxDealRequest.builder()
                .dealUniqueId(dealId)
                .fromCurrencyIsoCode("USD")
                .toCurrencyIsoCode("EUR")
                .dealTimestamp(LocalDateTime.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void getAllDeals_ReturnsAllDeals() {
        // Given
        FxDeal deal1 = createDeal("DEAL-001");
        FxDeal deal2 = createDeal("DEAL-002");
        when(repository.findAll()).thenReturn(Arrays.asList(deal1, deal2));

        // When
        List<FxDeal> results = service.getAllDeals();

        // Then
        assertEquals(2, results.size());
        verify(repository).findAll();
    }

    @Test
    void getAllDeals_ReturnsEmptyList() {
        // Given
        when(repository.findAll()).thenReturn(List.of());

        // When
        List<FxDeal> results = service.getAllDeals();

        // Then
        assertTrue(results.isEmpty());
        verify(repository).findAll();
    }

    @Test
    void importDeal_ValidatesBeforeCheckingDuplicate() {
        // Given
        doThrow(new InvalidDealException("Invalid amount"))
                .when(validator).validate(any());

        // When & Then
        assertThrows(InvalidDealException.class, () ->
                service.importDeal(validRequest)
        );
        verify(validator).validate(any());
        verify(repository, never()).existsByDealUniqueId(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void importDeal_WithNullRequest_ThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () ->
                service.importDeal(null)
        );
    }

    @Test
    void importDeals_WithEmptyList_ReturnsEmptyList() {
        // Given
        List<FxDealRequest> emptyList = List.of();

        // When
        List<FxDeal> results = service.importDeals(emptyList);

        // Then
        assertTrue(results.isEmpty());
        verify(repository, never()).save(any());
        verify(beanValidator, never()).validate(any());
    }

    @Test
    void importDeals_WithAllInvalidDeals_ReturnsEmptyList() {
        // Given
        FxDealRequest req1 = createRequest("DEAL-001");
        FxDealRequest req2 = createRequest("DEAL-002");
        List<FxDealRequest> requests = Arrays.asList(req1, req2);

        // Mock bean validator to return no violations
        when(beanValidator.validate(any(FxDealRequest.class)))
                .thenReturn(Collections.emptySet());

        doThrow(new InvalidDealException("Invalid"))
                .when(validator).validate(any());

        // When
        List<FxDeal> results = service.importDeals(requests);

        // Then
        assertTrue(results.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void importDeals_WithValidationAndDuplicateErrors_HandlesGracefully() {
        // Given
        FxDealRequest validReq = createRequest("DEAL-001");
        FxDealRequest invalidReq = createRequest("DEAL-002");
        FxDealRequest duplicateReq = createRequest("DEAL-003");
        List<FxDealRequest> requests = Arrays.asList(validReq, invalidReq, duplicateReq);

        // Mock bean validator - all pass bean validation
        when(beanValidator.validate(any(FxDealRequest.class)))
                .thenReturn(Collections.emptySet());

        // Valid deal succeeds
        doNothing().when(validator).validate(argThat(req ->
                "DEAL-001".equals(req.getDealUniqueId())));
        when(repository.existsByDealUniqueId("DEAL-001")).thenReturn(false);

        // Invalid deal fails validation
        doThrow(new InvalidDealException("Invalid currency"))
                .when(validator).validate(argThat(req ->
                        "DEAL-002".equals(req.getDealUniqueId())));

        // Duplicate deal fails
        doNothing().when(validator).validate(argThat(req ->
                "DEAL-003".equals(req.getDealUniqueId())));
        when(repository.existsByDealUniqueId("DEAL-003")).thenReturn(true);

        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });

        // When
        List<FxDeal> results = service.importDeals(requests);

        // Then
        assertEquals(1, results.size());
        assertEquals("DEAL-001", results.get(0).getDealUniqueId());
        verify(repository, times(1)).save(any(FxDeal.class));
    }

    @Test
    void importDeal_MapsAllFieldsCorrectly() {
        // Given
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30);
        BigDecimal amount = new BigDecimal("12345.67");

        validRequest.setDealTimestamp(timestamp);
        validRequest.setDealAmount(amount);
        validRequest.setFromCurrencyIsoCode("GBP");
        validRequest.setToCurrencyIsoCode("JPY");

        when(repository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });
        doNothing().when(validator).validate(any());

        // When
        FxDeal result = service.importDeal(validRequest);

        // Then
        assertEquals("DEAL-001", result.getDealUniqueId());
        assertEquals("GBP", result.getFromCurrencyIsoCode());
        assertEquals("JPY", result.getToCurrencyIsoCode());
        assertEquals(timestamp, result.getDealTimestamp());
        assertEquals(amount, result.getDealAmount());
    }

    @Test
    void importDeal_WithRepositoryException_ThrowsException() {
        // Given
        when(repository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(repository.save(any(FxDeal.class)))
                .thenThrow(new RuntimeException("Database error"));
        doNothing().when(validator).validate(any());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                service.importDeal(validRequest)
        );
    }

    @Test
    void importDeals_WithSingleDeal_ImportsSuccessfully() {
        // Given
        List<FxDealRequest> requests = List.of(validRequest);

        // Mock bean validator to return no violations
        when(beanValidator.validate(any(FxDealRequest.class)))
                .thenReturn(Collections.emptySet());

        when(repository.existsByDealUniqueId(anyString())).thenReturn(false);
        when(repository.save(any(FxDeal.class))).thenAnswer(i -> {
            FxDeal deal = i.getArgument(0);
            deal.setId(1L);
            return deal;
        });
        doNothing().when(validator).validate(any());

        // When
        List<FxDeal> results = service.importDeals(requests);

        // Then
        assertEquals(1, results.size());
        verify(repository).save(any(FxDeal.class));
        verify(beanValidator).validate(any(FxDealRequest.class));
    }

    private FxDeal createDeal(String dealId) {
        return FxDeal.builder()
                .id(1L)
                .dealUniqueId(dealId)
                .fromCurrencyIsoCode("USD")
                .toCurrencyIsoCode("EUR")
                .dealTimestamp(LocalDateTime.now())
                .dealAmount(new BigDecimal("1000.00"))
                .build();
    }
}