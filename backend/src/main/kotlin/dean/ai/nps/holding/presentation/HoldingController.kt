package dean.ai.nps.holding.presentation

import dean.ai.nps.common.PageResponse
import dean.ai.nps.holding.application.HoldingQueryService
import dean.ai.nps.holding.domain.Market
import dean.ai.nps.holding.presentation.dto.HoldingResponse
import dean.ai.nps.holding.presentation.dto.HoldingSummaryResponse
import dean.ai.nps.holding.presentation.dto.HoldingTrendResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/holdings")
@CrossOrigin(origins = ["http://localhost:3000"])
class HoldingController(
    private val service: HoldingQueryService,
) {
    @GetMapping
    fun list(
        @RequestParam year: Int,
        @RequestParam(required = false) market: Market?,
        @RequestParam(required = false) q: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<HoldingResponse> = service.search(year, market, q, pageable)

    @GetMapping("/{stockCode}")
    fun detail(
        @PathVariable stockCode: String,
        @RequestParam year: Int,
    ): ResponseEntity<HoldingResponse> =
        service.detail(stockCode, year)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @GetMapping("/{stockCode}/trend")
    fun trend(@PathVariable stockCode: String): ResponseEntity<HoldingTrendResponse> =
        service.trend(stockCode)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @GetMapping("/summary")
    fun summary(@RequestParam year: Int): HoldingSummaryResponse = service.summary(year)
}
