package dean.ai.nps.holding.application

import dean.ai.nps.common.PageResponse
import dean.ai.nps.holding.domain.Market
import dean.ai.nps.holding.infrastructure.HoldingJpaEntity
import dean.ai.nps.holding.infrastructure.HoldingRepository
import dean.ai.nps.holding.infrastructure.StockRepository
import dean.ai.nps.holding.presentation.dto.AssetClassSummary
import dean.ai.nps.holding.presentation.dto.HoldingResponse
import dean.ai.nps.holding.presentation.dto.HoldingSummaryResponse
import dean.ai.nps.holding.presentation.dto.HoldingTrendPoint
import dean.ai.nps.holding.presentation.dto.HoldingTrendResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class HoldingQueryService(
    private val holdingRepository: HoldingRepository,
    private val stockRepository: StockRepository,
) {
    fun search(year: Int, market: Market?, query: String?, pageable: Pageable): PageResponse<HoldingResponse> {
        val page = holdingRepository.search(year, market, query?.takeIf { it.isNotBlank() }, pageable)
        val stockCache = stockRepository.findAllById(page.content.map { it.stockCode }).associateBy { it.code }
        return PageResponse.of(page) { entity ->
            val previous = holdingRepository.findByStockCodeAndFiscalYear(entity.stockCode, entity.fiscalYear - 1)
            toResponse(entity, stockCache[entity.stockCode]?.name ?: entity.stockCode, stockCache[entity.stockCode]?.market ?: Market.UNKNOWN, previous)
        }
    }

    fun detail(stockCode: String, year: Int): HoldingResponse? {
        val entity = holdingRepository.findByStockCodeAndFiscalYear(stockCode, year) ?: return null
        val stock = stockRepository.findById(stockCode).orElse(null)
        val previous = holdingRepository.findByStockCodeAndFiscalYear(stockCode, year - 1)
        return toResponse(entity, stock?.name ?: stockCode, stock?.market ?: Market.UNKNOWN, previous)
    }

    fun trend(stockCode: String): HoldingTrendResponse? {
        val entities = holdingRepository.findAllByStockCodeOrderByFiscalYearAsc(stockCode)
        if (entities.isEmpty()) return null
        val stock = stockRepository.findById(stockCode).orElse(null)
        return HoldingTrendResponse(
            stockCode = stockCode,
            stockName = stock?.name ?: stockCode,
            market = stock?.market ?: Market.UNKNOWN,
            points = entities.map {
                HoldingTrendPoint(
                    fiscalYear = it.fiscalYear,
                    shares = it.shares,
                    marketValue = it.marketValue,
                    ownershipRatio = it.ownershipRatio,
                )
            },
        )
    }

    fun summary(year: Int): HoldingSummaryResponse {
        val buckets = holdingRepository.summaryByAssetClass(year).map {
            AssetClassSummary(it.assetClass, it.count, it.totalValue ?: BigDecimal.ZERO)
        }
        val top = holdingRepository.findTop10ByFiscalYearOrderByMarketValueDesc(year)
        val stockCache = stockRepository.findAllById(top.map { it.stockCode }).associateBy { it.code }
        val topResponses = top.map { entity ->
            val previous = holdingRepository.findByStockCodeAndFiscalYear(entity.stockCode, entity.fiscalYear - 1)
            toResponse(entity, stockCache[entity.stockCode]?.name ?: entity.stockCode, stockCache[entity.stockCode]?.market ?: Market.UNKNOWN, previous)
        }
        return HoldingSummaryResponse(year, buckets, topResponses)
    }

    private fun toResponse(
        entity: HoldingJpaEntity,
        stockName: String,
        market: Market,
        previous: HoldingJpaEntity?,
    ): HoldingResponse = HoldingResponse(
        stockCode = entity.stockCode,
        stockName = stockName,
        market = market,
        fiscalYear = entity.fiscalYear,
        shares = entity.shares,
        marketValue = entity.marketValue,
        ownershipRatio = entity.ownershipRatio,
        assetClass = entity.assetClass,
        yearOverYearChangeRatio = entity.toDomain().changeRatioFrom(previous?.toDomain()),
    )
}
