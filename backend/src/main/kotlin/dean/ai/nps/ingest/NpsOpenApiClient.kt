package dean.ai.nps.ingest

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class OdcloudResponse(
    val page: Int = 0,
    val perPage: Int = 0,
    val totalCount: Int = 0,
    val currentCount: Int = 0,
    val matchCount: Int = 0,
    val data: List<Map<String, Any?>> = emptyList(),
)

@Component
class NpsOpenApiClient(
    private val properties: NpsOpenApiProperties,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    fun fetchPage(year: Int, page: Int): OdcloudResponse {
        require(properties.serviceKey.isNotBlank()) {
            "nps.openapi.service-key is not configured (set NPS_SERVICE_KEY env)"
        }
        return restClient.get()
            .uri { builder ->
                builder.path("/${properties.datasetPath}")
                    .queryParam("serviceKey", properties.serviceKey)
                    .queryParam("page", page)
                    .queryParam("perPage", properties.perPage)
                    .queryParam("returnType", "JSON")
                    .build()
            }
            .retrieve()
            .body(OdcloudResponse::class.java)
            ?: OdcloudResponse()
    }

    fun fetchAll(year: Int): Sequence<Map<String, Any?>> = sequence {
        var page = 1
        while (true) {
            val response = fetchPage(year, page)
            if (response.data.isEmpty()) break
            yieldAll(response.data)
            val totalFetched = page * response.perPage
            if (totalFetched >= response.matchCount) break
            page++
        }
    }
}
