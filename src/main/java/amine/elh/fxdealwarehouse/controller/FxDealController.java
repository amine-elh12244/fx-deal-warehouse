package amine.elh.fxdealwarehouse.controller;


import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.model.FxDeal;
import amine.elh.fxdealwarehouse.service.FxDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
@Slf4j
public class FxDealController {

    private final FxDealService fxDealService;

    @PostMapping
    public ResponseEntity<FxDeal> importDeal(@Valid @RequestBody FxDealRequest request) {
        log.info("Received request to import deal: {}", request.getDealUniqueId());
        FxDeal deal = fxDealService.importDeal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(deal);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<FxDeal>> importDeals(
             @RequestBody List<FxDealRequest> requests) {
        log.info("Received bulk import request for {} deals", requests.size());
        List<FxDeal> deals = fxDealService.importDeals(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(deals);
    }

    @GetMapping
    public ResponseEntity<List<FxDeal>> getAllDeals() {
        log.info("Fetching all deals");
        List<FxDeal> deals = fxDealService.getAllDeals();
        return ResponseEntity.ok(deals);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("FX Deal Warehouse is running");
    }
}