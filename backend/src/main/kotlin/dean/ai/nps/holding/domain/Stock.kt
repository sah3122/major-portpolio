package dean.ai.nps.holding.domain

data class Stock(
    val code: String,
    val name: String,
    val market: Market,
) {
    init {
        require(code.isNotBlank()) { "stock code must not be blank" }
        require(name.isNotBlank()) { "stock name must not be blank" }
    }
}
