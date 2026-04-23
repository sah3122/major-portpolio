package dean.ai.nps.ingest

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class IngestionResultResponse(
    val id: Long?,
    val fiscalYear: Int,
    val status: IngestionStatus,
    val recordCount: Int,
    val errorMessage: String?,
)

@RestController
@RequestMapping("/api/ingest")
@CrossOrigin(origins = ["http://localhost:3000"])
class IngestionController(
    private val service: IngestionService,
) {
    @PostMapping("/run")
    fun run(@RequestParam year: Int): IngestionResultResponse {
        val result = service.ingestYear(year)
        return IngestionResultResponse(
            id = result.id,
            fiscalYear = result.fiscalYear,
            status = result.status,
            recordCount = result.recordCount,
            errorMessage = result.errorMessage,
        )
    }
}
