package dean.ai.nps.ingest

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nps.openapi")
data class NpsOpenApiProperties(
    val baseUrl: String = "https://api.odcloud.kr/api",
    val datasetPath: String = "3070507/v1/uddi:cc757223-fdc0-45b2-a617-dcbecec3fe1f",
    val serviceKey: String = "",
    val timeoutSeconds: Long = 30,
    val perPage: Int = 1000,
)
