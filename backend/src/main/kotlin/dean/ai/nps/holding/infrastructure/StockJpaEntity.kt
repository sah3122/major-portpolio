package dean.ai.nps.holding.infrastructure

import dean.ai.nps.holding.domain.Market
import dean.ai.nps.holding.domain.Stock
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "stocks")
class StockJpaEntity(
    @Id
    @Column(name = "code", length = 32)
    val code: String,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "market", nullable = false, length = 16)
    var market: Market,
) {
    fun toDomain(): Stock = Stock(code = code, name = name, market = market)

    companion object {
        fun fromDomain(stock: Stock): StockJpaEntity =
            StockJpaEntity(code = stock.code, name = stock.name, market = stock.market)
    }
}
