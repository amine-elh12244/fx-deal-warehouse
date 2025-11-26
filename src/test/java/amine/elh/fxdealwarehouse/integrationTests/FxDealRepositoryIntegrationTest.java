package amine.elh.fxdealwarehouse.integrationTests;



import amine.elh.fxdealwarehouse.model.FxDeal;
import amine.elh.fxdealwarehouse.repository.FxDealRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
class FxDealRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> db =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("fxdb_test")
                    .withUsername("amine")
                    .withPassword("amine");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.username", db::getUsername);
        registry.add("spring.datasource.password", db::getPassword);
    }

    @Autowired
    private FxDealRepository repository;

    private FxDeal deal;

    @BeforeEach
    void init() {
        deal = FxDeal.builder()
                .dealUniqueId("FX-TEST-100")
                .fromCurrencyIsoCode("USD")
                .toCurrencyIsoCode("EUR")
                .dealTimestamp(LocalDateTime.now())
                .dealAmount(new BigDecimal("125000.5000"))
                .build(); // importedAt handled by @PrePersist
    }

    @Test
    void shouldSaveFxDeal() {
        FxDeal saved = repository.save(deal);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDealUniqueId()).isEqualTo("FX-TEST-100");
        assertThat(saved.getDealAmount()).isEqualByComparingTo("125000.5000");
    }

    @Test
    void shouldReturnTrueWhenDealExists() {
        repository.save(deal);

        boolean exists = repository.existsByDealUniqueId("FX-TEST-100");

        assertThat(exists).isTrue();
    }

    @Test
    void shouldSetImportedAtOnPersist() {
        FxDeal saved = repository.save(deal);

        assertThat(saved.getImportedAt()).isNotNull();
    }
}
