package dean.ai.nps

import dean.ai.nps.ingest.NpsOpenApiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(NpsOpenApiProperties::class)
class NpsPortfolioApplication

fun main(args: Array<String>) {
    runApplication<NpsPortfolioApplication>(*args)
}
