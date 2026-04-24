package dean.ai.nps.ingest

import dean.ai.nps.holding.domain.AssetClass
import dean.ai.nps.holding.domain.Market
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class NpsDataParserSpec : DescribeSpec({
    val parser = NpsDataParser()

    describe("parse") {
        it("maps Korean field names to domain") {
            val raw = mapOf(
                "종목코드" to "005930",
                "종목명" to "삼성전자",
                "시장구분" to "유가증권",
                "보유수량" to "1,000,000",
                "평가액(억 원)" to "800",   // 800억 → 80,000,000,000 KRW
                "지분율(퍼센트)" to "7.56",
                "자산군" to "국내주식",
            )

            val record = parser.parse(raw, 2025)

            record.shouldNotBeNull()
            record.stock.code shouldBe "005930"
            record.stock.market shouldBe Market.KOSPI
            record.holding.shares shouldBe 1_000_000L
            record.holding.marketValue shouldBe BigDecimal("80000000000")
            record.holding.assetClass shouldBe AssetClass.DOMESTIC_EQUITY
        }

        it("infers asset class from market when missing") {
            val raw = mapOf(
                "stockCode" to "AAPL",
                "stockName" to "Apple Inc",
                "market" to "NASDAQ",
            )

            val record = parser.parse(raw, 2025)

            record.shouldNotBeNull()
            record.holding.assetClass shouldBe AssetClass.OVERSEAS_EQUITY
        }

        it("returns null when required fields missing") {
            parser.parse(mapOf("시장구분" to "유가증권"), 2025).shouldBeNull()
        }
    }
})
