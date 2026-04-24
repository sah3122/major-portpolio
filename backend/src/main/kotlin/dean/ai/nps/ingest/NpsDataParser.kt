package dean.ai.nps.ingest

import dean.ai.nps.holding.domain.AssetClass
import dean.ai.nps.holding.domain.Holding
import dean.ai.nps.holding.domain.Market
import dean.ai.nps.holding.domain.Stock
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class ParsedRecord(val stock: Stock, val holding: Holding)

@Component
class NpsDataParser {

    fun parse(raw: Map<String, Any?>, fiscalYear: Int): ParsedRecord? {
        val name = raw.pickString("종목명", "stockName", "name") ?: return null
        // API에 종목코드 없음 — 종목명을 코드 대신 사용
        val code = raw.pickString("종목코드", "stockCode", "isin") ?: name.toStockCode()

        val market = Market.from(raw.pickString("시장구분", "market"))
        // 이 데이터셋은 국내주식 전용이므로 시장 정보 없을 때 KOSPI 기본값
        val resolvedMarket = if (market == Market.UNKNOWN) Market.KOSPI else market

        // 평가액 단위가 억 원 → KRW 변환
        val marketValueEok = raw.pickBigDecimal("평가액(억 원)", "평가액", "평가금액", "marketValue")
            ?: BigDecimal.ZERO
        val marketValue = marketValueEok.multiply(BigDecimal("100000000"))

        val ownershipRatio = raw.pickBigDecimal("지분율(퍼센트)", "지분율", "ownershipRatio")
            ?: BigDecimal.ZERO

        val assetClass = AssetClass.from(raw.pickString("자산군", "assetClass"))
            .takeIf { it != AssetClass.UNKNOWN }
            ?: when (resolvedMarket) {
                Market.OVERSEAS -> AssetClass.OVERSEAS_EQUITY
                else -> AssetClass.DOMESTIC_EQUITY
            }

        val shares = raw.pickBigDecimal("보유수량", "shares")
            ?.toLong() ?: 0L

        return runCatching {
            ParsedRecord(
                stock = Stock(code, name, resolvedMarket),
                holding = Holding(
                    stockCode = code,
                    fiscalYear = fiscalYear,
                    shares = shares,
                    marketValue = marketValue,
                    ownershipRatio = ownershipRatio,
                    assetClass = assetClass,
                ),
            )
        }.getOrNull()
    }

    private fun String.toStockCode(): String =
        this.trim()
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9가-힣_]"), "")
            .take(32)

    private fun Map<String, Any?>.pickString(vararg keys: String): String? {
        for (k in keys) {
            val v = this[k]
            if (v != null) return v.toString().trim().takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun Map<String, Any?>.pickBigDecimal(vararg keys: String): BigDecimal? {
        val raw = pickString(*keys) ?: return null
        return runCatching { BigDecimal(raw.replace(",", "")) }.getOrNull()
    }
}
