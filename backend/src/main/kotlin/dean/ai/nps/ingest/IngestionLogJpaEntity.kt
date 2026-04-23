package dean.ai.nps.ingest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

enum class IngestionStatus { SUCCESS, FAILURE, RUNNING }

@Entity
@Table(
    name = "ingestion_logs",
    indexes = [Index(name = "idx_ingestion_year", columnList = "fiscal_year")],
)
class IngestionLogJpaEntity(
    @Column(name = "source", nullable = false, length = 64)
    var source: String,

    @Column(name = "fiscal_year", nullable = false)
    var fiscalYear: Int,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant,

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: IngestionStatus = IngestionStatus.RUNNING,

    @Column(name = "record_count", nullable = false)
    var recordCount: Int = 0,

    @Column(name = "error_message", length = 2000)
    var errorMessage: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
