package dean.ai.nps.ingest

import org.springframework.data.jpa.repository.JpaRepository

interface IngestionLogRepository : JpaRepository<IngestionLogJpaEntity, Long>
