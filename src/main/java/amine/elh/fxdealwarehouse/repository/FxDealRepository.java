package amine.elh.fxdealwarehouse.repository;


import amine.elh.fxdealwarehouse.model.FxDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FxDealRepository extends JpaRepository<FxDeal, Long> {

    boolean existsByDealUniqueId(String dealUniqueId);
}