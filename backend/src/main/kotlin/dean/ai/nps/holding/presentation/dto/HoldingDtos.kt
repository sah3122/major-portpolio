package dean.ai.nps.holding.presentation.dto

import dean.ai.nps.holding.domain.AssetClass
import dean.ai.nps.holding.domain.Market
import java.math.BigDecimal

data class HoldingResponse(
    val stockCode: String,
    val stockName: String,
    val market: Market,
    val fiscalYear: Int,
    val shares: Long,
    val marketValue: BigDecimal,
    val ownershipRatio: BigDecimal,
    val assetClass: AssetClass,
    val yearOverYearChangeRatio: BigDecimal?,
)

data class HoldingTrendPoint(
    val fiscalYear: Int,
    val shares: Long,
    val marketValue: BigDecimal,
    val ownershipRatio: BigDecimal,
)

data class HoldingTrendResponse(
    val stockCode: String,
    val stockName: String,
    val market: Market,
    val points: List<HoldingTrendPoint>,
)

data class AssetClassSummary(
    val assetClass: AssetClass,
    val count: Long,
    val totalValue: BigDecimal,
)

data class HoldingSummaryResponse(
    val fiscalYear: Int,
    val assetClasses: List<AssetClassSummary>,
    val topHoldings: List<HoldingResponse>,
)
