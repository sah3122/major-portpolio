package dean.ai.nps.holding.domain

import java.math.BigDecimal

data class Holding(
    val stockCode: String,
    val fiscalYear: Int,
    val shares: Long,
    val marketValue: BigDecimal,
    val ownershipRatio: BigDecimal,
    val assetClass: AssetClass,
) {
    init {
        require(stockCode.isNotBlank()) { "stockCode must not be blank" }
        require(fiscalYear in 2000..2100) { "fiscalYear out of range: $fiscalYear" }
        require(shares >= 0) { "shares must not be negative" }
        require(marketValue.signum() >= 0) { "marketValue must not be negative" }
        require(ownershipRatio.signum() >= 0) { "ownershipRatio must not be negative" }
    }

    fun changeRatioFrom(previous: Holding?): BigDecimal? {
        if (previous == null || previous.shares == 0L) return null
        val diff = (shares - previous.shares).toBigDecimal()
        return diff.divide(previous.shares.toBigDecimal(), 6, java.math.RoundingMode.HALF_UP)
    }
}
