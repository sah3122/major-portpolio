package dean.ai.nps.holding.domain

enum class Market {
    KOSPI,
    KOSDAQ,
    OVERSEAS,
    UNKNOWN;

    companion object {
        fun from(raw: String?): Market = when (raw?.trim()?.uppercase()) {
            "KOSPI", "유가증권", "KRX", "코스피" -> KOSPI
            "KOSDAQ", "코스닥" -> KOSDAQ
            "OVERSEAS", "해외", "NYSE", "NASDAQ" -> OVERSEAS
            else -> UNKNOWN
        }
    }
}
