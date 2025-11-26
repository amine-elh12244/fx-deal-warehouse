package amine.elh.fxdealwarehouse.service;

import amine.elh.fxdealwarehouse.dto.FxDealRequest;
import amine.elh.fxdealwarehouse.model.FxDeal;

import java.util.List;

public interface FxDealService {
    FxDeal importDeal(FxDealRequest request);
    List<FxDeal> importDeals(List<FxDealRequest> requests);
    List<FxDeal> getAllDeals();
}