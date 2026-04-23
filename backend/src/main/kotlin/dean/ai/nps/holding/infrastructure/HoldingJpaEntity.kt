package dean.ai.nps.holding.infrastructure

import dean.ai.nps.holding.domain.AssetClass
import dean.ai.nps.holding.domain.Holding
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal

@Entity
@Table(
    name = "holdings",
    uniqueConstraints = [UniqueConstraint(name = "uk_holdings_stock_year", columnNames = ["stock_code", "fiscal_year"])],
    indexes = [
        Index(name = "idx_holdings_year", columnList = "fiscal_year"),
        Index(name = "idx_holdings_market_value", columnList = "fiscal_year, market_value"),
    ],
)
class HoldingJpaEntity(
    @Column(name = "stock_code", nullable = false, length = 32)
    var stockCode: String,

    @Column(name = "fiscal_year", nullable = false)
    var fiscalYear: Int,

    @Column(name = "shares", nullable = false)
    var shares: Long,

    @Column(name = "market_value", nullable = false, precision = 24, scale = 2)
    var marketValue: BigDecimal,

    @Column(name = "ownership_ratio", nullable = false, precision = 10, scale = 6)
    var ownershipRatio: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 32)
    var assetClass: AssetClass,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    fun toDomain(): Holding = Holding(
        stockCode = stockCode,
        fiscalYear = fiscalYear,
        shares = shares,
        marketValue = marketValue,
        ownershipRatio = ownershipRatio,
        assetClass = assetClass,
    )

    fun update(holding: Holding) {
        this.shares = holding.shares
        this.marketValue = holding.marketValue
        this.ownershipRatio = holding.ownershipRatio
        this.assetClass = holding.assetClass
    }

    companion object {
        fun fromDomain(holding: Holding): HoldingJpaEntity = HoldingJpaEntity(
            stockCode = holding.stockCode,
            fiscalYear = holding.fiscalYear,
            shares = holding.shares,
            marketValue = holding.marketValue,
            ownershipRatio = holding.ownershipRatio,
            assetClass = holding.assetClass,
        )
    }
}
