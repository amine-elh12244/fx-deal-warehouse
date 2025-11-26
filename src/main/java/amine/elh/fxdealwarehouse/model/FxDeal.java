package amine.elh.fxdealwarehouse.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fx_deals", indexes = {
        @Index(name = "idx_deal_unique_id", columnList = "dealUniqueId", unique = true),
        @Index(name = "idx_deal_timestamp", columnList = "dealTimestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String dealUniqueId;

    @Column(nullable = false, length = 3)
    private String fromCurrencyIsoCode;

    @Column(nullable = false, length = 3)
    private String toCurrencyIsoCode;

    @Column(nullable = false)
    private LocalDateTime dealTimestamp;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal dealAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}