package dean.ai.nps.holding.infrastructure

import dean.ai.nps.holding.domain.Market
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface HoldingRepository : JpaRepository<HoldingJpaEntity, Long> {

    fun findByStockCodeAndFiscalYear(stockCode: String, fiscalYear: Int): HoldingJpaEntity?

    fun findAllByStockCodeOrderByFiscalYearAsc(stockCode: String): List<HoldingJpaEntity>

    @Query(
        """
        SELECT h FROM HoldingJpaEntity h
        JOIN StockJpaEntity s ON s.code = h.stockCode
        WHERE h.fiscalYear = :year
          AND (:market IS NULL OR s.market = :market)
          AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR s.code LIKE CONCAT('%', :q, '%'))
        """
    )
    fun search(
        @Param("year") year: Int,
        @Param("market") market: Market?,
        @Param("q") q: String?,
        pageable: Pageable,
    ): Page<HoldingJpaEntity>

    @Query(
        """
        SELECT h.assetClass AS assetClass,
               COUNT(h) AS count,
               SUM(h.marketValue) AS totalValue
        FROM HoldingJpaEntity h
        WHERE h.fiscalYear = :year
        GROUP BY h.assetClass
        """
    )
    fun summaryByAssetClass(@Param("year") year: Int): List<AssetClassSummaryRow>

    fun findTop10ByFiscalYearOrderByMarketValueDesc(fiscalYear: Int): List<HoldingJpaEntity>
}

interface AssetClassSummaryRow {
    val assetClass: dean.ai.nps.holding.domain.AssetClass
    val count: Long
    val totalValue: java.math.BigDecimal?
}
