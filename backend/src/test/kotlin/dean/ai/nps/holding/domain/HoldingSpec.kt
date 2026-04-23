package dean.ai.nps.holding.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class HoldingSpec : DescribeSpec({
    describe("Holding") {
        it("rejects blank stock code") {
            shouldThrow<IllegalArgumentException> {
                Holding("", 2025, 100, BigDecimal.TEN, BigDecimal.ONE, AssetClass.DOMESTIC_EQUITY)
            }
        }

        it("rejects fiscal year out of range") {
            shouldThrow<IllegalArgumentException> {
                Holding("005930", 1800, 100, BigDecimal.TEN, BigDecimal.ONE, AssetClass.DOMESTIC_EQUITY)
            }
        }

        it("rejects negative shares") {
            shouldThrow<IllegalArgumentException> {
                Holding("005930", 2025, -1, BigDecimal.TEN, BigDecimal.ONE, AssetClass.DOMESTIC_EQUITY)
            }
        }

        describe("changeRatioFrom") {
            val current = Holding("005930", 2025, 120, BigDecimal("1200"), BigDecimal("10"), AssetClass.DOMESTIC_EQUITY)

            it("returns null when previous is null") {
                current.changeRatioFrom(null).shouldBeNull()
            }

            it("returns null when previous shares were zero") {
                val previous = current.copy(fiscalYear = 2024, shares = 0)
                current.changeRatioFrom(previous).shouldBeNull()
            }

            it("computes relative change") {
                val previous = current.copy(fiscalYear = 2024, shares = 100)
                current.changeRatioFrom(previous) shouldBe BigDecimal("0.200000")
            }

            it("returns negative ratio when shares decreased") {
                val previous = current.copy(fiscalYear = 2024, shares = 150)
                current.changeRatioFrom(previous) shouldBe BigDecimal("-0.200000")
            }
        }
    }

    describe("Market.from") {
        it("maps Korean labels") {
            Market.from("유가증권") shouldBe Market.KOSPI
            Market.from("코스닥") shouldBe Market.KOSDAQ
        }
        it("defaults to UNKNOWN for null or unexpected values") {
            Market.from(null) shouldBe Market.UNKNOWN
            Market.from("foo") shouldBe Market.UNKNOWN
        }
    }
})
