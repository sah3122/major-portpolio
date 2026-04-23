package dean.ai.nps.ingest

import dean.ai.nps.holding.infrastructure.HoldingJpaEntity
import dean.ai.nps.holding.infrastructure.HoldingRepository
import dean.ai.nps.holding.infrastructure.StockJpaEntity
import dean.ai.nps.holding.infrastructure.StockRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class IngestionService(
    private val client: NpsOpenApiClient,
    private val parser: NpsDataParser,
    private val stockRepository: StockRepository,
    private val holdingRepository: HoldingRepository,
    private val ingestionLogRepository: IngestionLogRepository,
) {
    private val log = LoggerFactory.getLogger(IngestionService::class.java)

    @Transactional
    fun ingestYear(year: Int): IngestionLogJpaEntity {
        val logEntry = ingestionLogRepository.save(
            IngestionLogJpaEntity(
                source = "data.go.kr",
                fiscalYear = year,
                startedAt = Instant.now(),
            )
        )
        try {
            var count = 0
            for (raw in client.fetchAll(year)) {
                val parsed = parser.parse(raw, year) ?: continue
                upsert(parsed)
                count++
            }
            logEntry.recordCount = count
            logEntry.status = IngestionStatus.SUCCESS
        } catch (ex: Exception) {
            log.error("Ingestion failed for year {}", year, ex)
            logEntry.status = IngestionStatus.FAILURE
            logEntry.errorMessage = ex.message?.take(2000)
        } finally {
            logEntry.finishedAt = Instant.now()
        }
        return ingestionLogRepository.save(logEntry)
    }

    private fun upsert(parsed: ParsedRecord) {
        val stockEntity = stockRepository.findById(parsed.stock.code).orElseGet {
            StockJpaEntity(parsed.stock.code, parsed.stock.name, parsed.stock.market)
        }.apply {
            name = parsed.stock.name
            market = parsed.stock.market
        }
        stockRepository.save(stockEntity)

        val existing = holdingRepository.findByStockCodeAndFiscalYear(parsed.holding.stockCode, parsed.holding.fiscalYear)
        if (existing != null) {
            existing.update(parsed.holding)
        } else {
            holdingRepository.save(HoldingJpaEntity.fromDomain(parsed.holding))
        }
    }
}
