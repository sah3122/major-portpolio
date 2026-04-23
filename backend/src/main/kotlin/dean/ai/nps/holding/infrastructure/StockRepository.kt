package dean.ai.nps.holding.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<StockJpaEntity, String>
