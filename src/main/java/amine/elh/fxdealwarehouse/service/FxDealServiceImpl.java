package amine.elh.fxdealwarehouse.service;

import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.exception.DuplicateDealException;
import amine.elh.fxdealwarehouse.model.FxDeal;
import amine.elh.fxdealwarehouse.repository.FxDealRepository;
import amine.elh.fxdealwarehouse.validator.FxDealValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FxDealServiceImpl implements FxDealService {

    private final FxDealRepository repository;
    private final FxDealValidator validator;
    private final Validator beanValidator;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FxDeal importDeal(FxDealRequest request) {
        log.info("Importing FX deal: {}", request.getDealUniqueId());

        validator.validate(request);

        if (repository.existsByDealUniqueId(request.getDealUniqueId())) {
            log.warn("Duplicate deal detected: {}", request.getDealUniqueId());
            throw new DuplicateDealException(
                    "Deal with ID " + request.getDealUniqueId() + " already exists"
            );
        }

        FxDeal deal = mapToDeal(request);
        FxDeal savedDeal = repository.save(deal);

        log.info("Successfully imported deal: {}", savedDeal.getDealUniqueId());
        return savedDeal;
    }

    @Override
    public List<FxDeal> importDeals(List<FxDealRequest> requests) {
        log.info("Bulk importing {} deals", requests.size());
        List<FxDeal> importedDeals = new ArrayList<>();

        for (FxDealRequest request : requests) {
            try {
                Set<ConstraintViolation<FxDealRequest>> violations =
                        beanValidator.validate(request);

                if (!violations.isEmpty()) {
                    String violationMessages = violations.stream()
                            .map(ConstraintViolation::getMessage)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Validation failed");

                    log.error("Bean validation failed for deal {}: {}",
                            request.getDealUniqueId(), violationMessages);
                    continue;
                }

                FxDeal deal = importDeal(request);
                importedDeals.add(deal);
            } catch (Exception e) {
                log.error("Failed to import deal {}: {}",
                        request != null ? request.getDealUniqueId() : "null",
                        e.getMessage());
            }
        }

        log.info("Successfully imported {}/{} deals",
                importedDeals.size(), requests.size());
        return importedDeals;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FxDeal> getAllDeals() {
        return repository.findAll();
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