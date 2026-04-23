package dean.ai.nps.holding.domain

enum class AssetClass {
    DOMESTIC_EQUITY,
    OVERSEAS_EQUITY,
    DOMESTIC_BOND,
    OVERSEAS_BOND,
    ALTERNATIVE,
    CASH,
    UNKNOWN;

    companion object {
        fun from(raw: String?): AssetClass = when (raw?.trim()) {
            "국내주식" -> DOMESTIC_EQUITY
            "해외주식" -> OVERSEAS_EQUITY
            "국내채권" -> DOMESTIC_BOND
            "해외채권" -> OVERSEAS_BOND
            "대체투자" -> ALTERNATIVE
            "현금성자산", "현금" -> CASH
            else -> UNKNOWN
        }
    }
}
