# Task: DART 대량보유보고서 수집 파이프라인

## 상태
- 작성일: 2026-04-23
- 최종 수정: 2026-04-23
- 상태: Ready
- 참조 Plan: `tasks/plan-dart-pipeline.md`

---

## Phase 개요

| Phase | 이름 | 상태 |
|-------|------|------|
| Phase 1 | 도메인 — DartDisclosure 도메인 객체 | Ready |
| Phase 2 | 인프라 — JPA 엔티티, Repository, DART API 클라이언트 | Ready |
| Phase 3 | 애플리케이션 — Parser, IngestionService, 스케줄러 | Ready |
| Phase 4 | 프레젠테이션 (백엔드) — Controller·DTO·QueryService 확장 | Ready |
| Phase 5 | 프론트엔드 — 공시 이력 테이블 | Ready |

---

## Phase 1. 도메인 — DartDisclosure 도메인 객체

> DART 대량보유보고서 1건을 표현하는 순수 도메인 객체와 열거형을 정의한다.

### 작업 목록

- [ ] T1-1. `ReportType` 열거형 정의
- [ ] T1-2. `DartDisclosure` 도메인 객체 정의
- [ ] T1-3. 도메인 단위 테스트 작성

---

### T1-1. ReportType 열거형

**파일**: `holding/domain/DartDisclosure.kt` (신규)

```kotlin
enum class ReportType {
    NEW,    // 신규 보고
    CHANGE; // 변동 보고

    companion object {
        fun from(raw: String?): ReportType = when (raw?.trim()) {
            "신규", "NEW" -> NEW
            else -> CHANGE
        }
    }
}
```

---

### T1-2. DartDisclosure 도메인 객체

**파일**: `holding/domain/DartDisclosure.kt` (신규, T1-1과 동일 파일)

```kotlin
data class DartDisclosure(
    val rceptNo: String,
    val stockCode: String,
    val stockName: String,
    val rceptDate: LocalDate,
    val reportType: ReportType,
    val ownershipRatio: BigDecimal,
    val shares: Long,
    val belowFivePercent: Boolean,
) {
    init {
        require(rceptNo.isNotBlank()) { "rceptNo must not be blank" }
        require(stockCode.isNotBlank()) { "stockCode must not be blank" }
        require(ownershipRatio.signum() >= 0) { "ownershipRatio must not be negative" }
        require(shares >= 0) { "shares must not be negative" }
    }
}
```

---

### T1-3. 도메인 단위 테스트

**파일**: `src/test/kotlin/dean/ai/nps/holding/domain/DartDisclosureSpec.kt` (신규)

```kotlin
class DartDisclosureSpec : DescribeSpec({
    describe("DartDisclosure 생성") {
        it("유효한 값으로 생성 성공") { /* 정상 케이스 */ }
        it("rceptNo가 blank이면 예외") { /* 검증 케이스 */ }
        it("ownershipRatio가 음수이면 예외") { /* 검증 케이스 */ }
        it("shares가 음수이면 예외") { /* 검증 케이스 */ }
    }

    describe("ReportType.from") {
        it("'신규' → ReportType.NEW") { }
        it("'변동' → ReportType.CHANGE") { }
        it("알 수 없는 값 → ReportType.CHANGE (기본값)") { }
    }
})
```

### Phase 1 완료 조건

- [ ] T1-1 ~ T1-3 완료
- [ ] `./gradlew test` 통과
- [ ] Spring 컨텍스트 의존 없음 확인

---

## Phase 2. 인프라 — JPA 엔티티, Repository, DART API 클라이언트

> DB 저장 구조와 DART API 호출 클라이언트를 구현한다. Phase 1 완료 후 시작.

### 작업 목록

- [ ] T2-1. `DartOpenApiProperties` 설정 바인딩
- [ ] T2-2. `DartDisclosureJpaEntity` JPA 엔티티
- [ ] T2-3. `DartDisclosureRepository` JPA Repository
- [ ] T2-4. `DartOpenApiClient` HTTP 클라이언트
- [ ] T2-5. `application.yml` / `application-local.yml` 설정 추가

---

### T2-1. DartOpenApiProperties

**파일**: `ingest/DartOpenApiProperties.kt` (신규)

```kotlin
@ConfigurationProperties(prefix = "dart.openapi")
data class DartOpenApiProperties(
    val baseUrl: String,
    val apiKey: String,
    val corpCode: String,   // 국민연금: "00131747"
)
```

---

### T2-2. DartDisclosureJpaEntity

**파일**: `holding/infrastructure/DartDisclosureJpaEntity.kt` (신규)

```kotlin
@Entity
@Table(
    name = "dart_disclosures",
    uniqueConstraints = [UniqueConstraint(name = "uk_dart_rcept_no", columnNames = ["rcept_no"])],
    indexes = [
        Index(name = "idx_dart_stock_code", columnList = "stock_code"),
        Index(name = "idx_dart_rcept_date", columnList = "rcept_date"),
    ],
)
class DartDisclosureJpaEntity(
    @Column(name = "rcept_no", nullable = false, length = 20)
    val rceptNo: String,

    @Column(name = "stock_code", nullable = false, length = 32)
    val stockCode: String,

    @Column(name = "stock_name", nullable = false, length = 128)
    val stockName: String,

    @Column(name = "rcept_date", nullable = false)
    val rceptDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 16)
    val reportType: ReportType,

    @Column(name = "ownership_ratio", nullable = false, precision = 10, scale = 6)
    val ownershipRatio: BigDecimal,

    @Column(name = "shares", nullable = false)
    val shares: Long,

    @Column(name = "below_five_percent", nullable = false)
    val belowFivePercent: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun toDomain(): DartDisclosure = DartDisclosure(
        rceptNo, stockCode, stockName, rceptDate,
        reportType, ownershipRatio, shares, belowFivePercent,
    )

    companion object {
        fun fromDomain(d: DartDisclosure): DartDisclosureJpaEntity = DartDisclosureJpaEntity(
            rceptNo = d.rceptNo, stockCode = d.stockCode, stockName = d.stockName,
            rceptDate = d.rceptDate, reportType = d.reportType, ownershipRatio = d.ownershipRatio,
            shares = d.shares, belowFivePercent = d.belowFivePercent,
        )
    }
}
```

---

### T2-3. DartDisclosureRepository

**파일**: `holding/infrastructure/DartDisclosureRepository.kt` (신규)

```kotlin
interface DartDisclosureRepository : JpaRepository<DartDisclosureJpaEntity, Long> {
    fun existsByRceptNo(rceptNo: String): Boolean
    fun findByStockCodeOrderByRceptDateDesc(stockCode: String): List<DartDisclosureJpaEntity>
    fun findTopByStockCodeOrderByRceptDateDesc(stockCode: String): DartDisclosureJpaEntity?
    fun findTopByOrderByRceptDateDesc(): DartDisclosureJpaEntity?   // 마지막 수집일 조회용
}
```

---

### T2-4. DartOpenApiClient

**파일**: `ingest/DartOpenApiClient.kt` (신규)

DART `/api/majorstock.json` 을 호출하여 원시 응답을 반환한다.  
날짜 범위(`bgn_de`, `end_de`)를 파라미터로 받는다.

```kotlin
@Component
class DartOpenApiClient(private val properties: DartOpenApiProperties) {

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    // 응답 래퍼
    data class DartResponse(
        val status: String,
        val message: String,
        val list: List<Map<String, Any?>> = emptyList(),
    )

    fun fetchDisclosures(bgnDe: String, endDe: String): List<Map<String, Any?>> {
        // GET /api/majorstock.json?crtfc_key=...&corp_code=...&bgn_de=...&end_de=...
        // status != "000" 이면 빈 리스트 반환 (오류 로깅)
    }
}
```

---

### T2-5. 설정 추가

**파일**: `src/main/resources/application.yml` (수정)

```yaml
dart:
  openapi:
    base-url: https://opendart.fss.or.kr/api
    api-key: ${DART_API_KEY:}
    corp-code: "00131747"
```

**파일**: `src/main/resources/application-local.yml` (수정)  
로컬 테스트용 실제 API 키 추가 (git 제외).

### Phase 2 완료 조건

- [ ] T2-1 ~ T2-5 완료
- [ ] `./gradlew build` 통과 (컴파일 에러 없음)
- [ ] H2 인메모리 DB에서 `dart_disclosures` 테이블 자동 생성 확인 (`spring.jpa.show-sql=true`)

---

## Phase 3. 애플리케이션 — Parser, IngestionService, 스케줄러

> DART 응답을 도메인으로 변환하고, 수집 오케스트레이션과 자동 스케줄러를 구현한다. Phase 2 완료 후 시작.

### 작업 목록

- [ ] T3-1. `DartDataParser` 구현 및 테스트
- [ ] T3-2. `DartIngestionService` 구현 (수동 수집 + 자동 스케줄러)

---

### T3-1. DartDataParser

**파일**: `ingest/DartDataParser.kt` (신규)

DART API 원시 응답(`Map<String, Any?>`) 1건을 `DartDisclosure`로 변환한다.

```kotlin
@Component
class DartDataParser {
    fun parse(raw: Map<String, Any?>): DartDisclosure? {
        val rceptNo = raw["rcept_no"]?.toString()?.trim() ?: return null
        val stockCode = raw["stock_code"]?.toString()?.trim() ?: return null
        val stockName = raw["corp_name"]?.toString()?.trim() ?: return null
        val rceptDate = raw["rcept_dt"]?.toString()
            ?.let { LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE) } ?: return null
        val reportType = ReportType.from(raw["report_tp"]?.toString())
        val ownershipRatio = raw["holding_ratio"]?.toString()
            ?.let { runCatching { BigDecimal(it.replace(",", "")) }.getOrNull() } ?: BigDecimal.ZERO
        val shares = raw["number_of_voting_right"]?.toString()
            ?.replace(",", "")?.toLongOrNull() ?: 0L
        val belowFivePercent = ownershipRatio < BigDecimal("5.0")

        return runCatching {
            DartDisclosure(rceptNo, stockCode, stockName, rceptDate,
                reportType, ownershipRatio, shares, belowFivePercent)
        }.getOrNull()
    }
}
```

**테스트**: `src/test/kotlin/dean/ai/nps/ingest/DartDataParserSpec.kt` (신규)

```kotlin
class DartDataParserSpec : DescribeSpec({
    val parser = DartDataParser()

    describe("parse") {
        it("유효한 DART 응답 → DartDisclosure 반환") {
            // given: 정상 필드를 가진 Map
            // when: parser.parse(raw)
            // then: 각 필드 값 검증
        }
        it("rcept_no 누락 → null 반환") { }
        it("stock_code 누락 → null 반환") { }
        it("rcept_dt 파싱 실패 → null 반환") { }
        it("holding_ratio < 5.0 → belowFivePercent = true") { }
        it("holding_ratio >= 5.0 → belowFivePercent = false") { }
    }
})
```

---

### T3-2. DartIngestionService

**파일**: `ingest/DartIngestionService.kt` (신규)

수동 수집과 일 1회 자동 수집을 담당한다.  
이미 수집된 `rcept_no`는 `existsByRceptNo()`로 건너뛴다.

```kotlin
@Service
class DartIngestionService(
    private val client: DartOpenApiClient,
    private val parser: DartDataParser,
    private val disclosureRepository: DartDisclosureRepository,
    private val ingestionLogRepository: IngestionLogRepository,
) {
    private val log = LoggerFactory.getLogger(DartIngestionService::class.java)

    // 수동 수집: 마지막 수집일 다음 날 ~ 오늘까지 조회
    @Transactional
    fun ingest(): IngestionLogJpaEntity {
        val lastDate = disclosureRepository.findTopByOrderByRceptDateDesc()
            ?.rceptDate ?: LocalDate.of(2010, 1, 1)   // 최초 수집 시 전체 기간
        val bgnDe = lastDate.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
        val endDe = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return ingestRange(bgnDe, endDe)
    }

    // 일 1회 자동 수집 (매일 오전 7시)
    @Scheduled(cron = "0 0 7 * * *")
    fun scheduledIngest() {
        runCatching { ingest() }
            .onFailure { log.error("Scheduled DART ingestion failed", it) }
    }

    private fun ingestRange(bgnDe: String, endDe: String): IngestionLogJpaEntity {
        // 1. ingestion_log 시작 기록 (source = "dart.fss.or.kr")
        // 2. client.fetchDisclosures(bgnDe, endDe) 호출
        // 3. 각 항목 parser.parse() → null이면 skip
        // 4. existsByRceptNo() == true이면 skip (중복 방지)
        // 5. disclosureRepository.save()
        // 6. 성공/실패 log 업데이트
    }
}
```

`@EnableScheduling`을 `NpsPortfolioApplication.kt`에 추가한다.

### Phase 3 완료 조건

- [ ] T3-1 ~ T3-2 완료
- [ ] `./gradlew test` 통과
- [ ] 로컬에서 `curl -X POST http://localhost:8080/api/ingest/dart` 호출 시 `dart_disclosures` 테이블에 데이터 적재 확인 (Phase 4 완료 후 가능)

---

## Phase 4. 프레젠테이션 (백엔드) — Controller·DTO·QueryService 확장

> 수집 트리거 API 추가와 기존 holding 응답에 DART 데이터를 병합한다. Phase 3 완료 후 시작.

### 작업 목록

- [ ] T4-1. `IngestionController` — DART 수집 엔드포인트 추가
- [ ] T4-2. `HoldingDtos` — `HoldingTrendResponse`, `HoldingResponse` DTO 확장
- [ ] T4-3. `HoldingQueryService` — DART 데이터 조합 로직 추가

---

### T4-1. IngestionController 확장

**파일**: `ingest/IngestionController.kt` (수정)

```kotlin
@PostMapping("/dart")
fun runDart(): IngestionLogJpaEntity = dartIngestionService.ingest()
```

`DartIngestionService`를 생성자 주입으로 추가한다.

---

### T4-2. HoldingDtos 확장

**파일**: `holding/presentation/dto/HoldingDtos.kt` (수정)

**신규 DTO 추가**:
```kotlin
data class DartDisclosureItem(
    val rceptDate: String,           // "2024-03-15"
    val reportType: String,          // "NEW" | "CHANGE"
    val ownershipRatio: BigDecimal,
    val shares: Long,
    val belowFivePercent: Boolean,
    val source: String = "DART",
)
```

**기존 DTO 수정**:
```kotlin
// HoldingTrendResponse에 추가
data class HoldingTrendResponse(
    val stockCode: String,
    val stockName: String,
    val trend: List<YearlyHolding>,
    val dartDisclosures: List<DartDisclosureItem>,  // 신규
)

// HoldingResponse에 추가
data class HoldingResponse(
    // ... 기존 필드 유지 ...
    val latestDartDisclosure: DartDisclosureItem?,  // 신규, nullable
)
```

---

### T4-3. HoldingQueryService 확장

**파일**: `holding/application/HoldingQueryService.kt` (수정)

`DartDisclosureRepository`를 주입받아 `trend()`와 `detail()` 응답에 DART 데이터를 추가한다.

```kotlin
// trend() 내부 추가
fun trend(stockCode: String): HoldingTrendResponse? {
    // 기존 로직으로 trend 리스트 조회
    val dartItems = disclosureRepository
        .findByStockCodeOrderByRceptDateDesc(stockCode)
        .map { it.toDomain().toDto() }
    return HoldingTrendResponse(stockCode, stockName, trend, dartItems)
}

// detail() 내부 추가
fun detail(stockCode: String, year: Int): HoldingResponse? {
    // 기존 로직 + latestDartDisclosure 조합
    val latest = disclosureRepository
        .findTopByStockCodeOrderByRceptDateDesc(stockCode)
        ?.toDomain()?.toDto()
    return existing?.toResponse()?.copy(latestDartDisclosure = latest)
}
```

데이터 우선순위: 공공데이터포털 보유 데이터는 기존 필드에 그대로 유지하고, DART 데이터는 별도 필드로 병기한다. (덮어쓰기 없음)

### Phase 4 완료 조건

- [ ] T4-1 ~ T4-3 완료
- [ ] `./gradlew build` 통과
- [ ] `curl -X POST http://localhost:8080/api/ingest/dart` → 200 응답
- [ ] `curl http://localhost:8080/api/holdings/{stockCode}/trend` 응답에 `dartDisclosures` 배열 포함 확인
- [ ] `curl http://localhost:8080/api/holdings/{stockCode}?year=2024` 응답에 `latestDartDisclosure` 포함 확인

---

## Phase 5. 프론트엔드 — 공시 이력 테이블

> 종목 상세 페이지에 DART 공시 이력 테이블을 추가한다. Phase 4 완료 후 시작.

### 작업 목록

- [ ] T5-1. `DartDisclosureTable` 컴포넌트 구현
- [ ] T5-2. 종목 상세 페이지에 컴포넌트 통합

---

### T5-1. DartDisclosureTable 컴포넌트

**파일**: `frontend/app/holdings/[stockCode]/DartDisclosureTable.tsx` (신규)

표시 컬럼: 공시일 | 보고구분 | 보유비율 | 보유주식수 | 비고(5% 미만 배지)

- `reportType === "NEW"` → "신규" 뱃지
- `reportType === "CHANGE"` → "변동" 뱃지
- `belowFivePercent === true` → "5% 미만" 경고 배지

데이터 없을 때 빈 상태(empty state) 표시.

---

### T5-2. 종목 상세 페이지 통합

**파일**: `frontend/app/holdings/[stockCode]/page.tsx` (수정)

기존 연도별 추이 차트 아래에 `<DartDisclosureTable>` 추가.  
`trend` API 응답의 `dartDisclosures` 배열을 props로 전달.

### Phase 5 완료 조건

- [ ] T5-1 ~ T5-2 완료
- [ ] 종목 상세 페이지에서 DART 공시 이력 테이블 렌더링 확인
- [ ] 5% 미만 항목에 배지 표시 확인
- [ ] `dartDisclosures`가 빈 배열일 때 빈 상태 메시지 표시 확인
- [ ] 기존 차트·목록 기능 회귀 없음 확인

---

## 전체 완료 체크리스트

- [ ] Phase 1 ~ 5 모두 완료
- [ ] `./gradlew test` 전체 통과
- [ ] `curl -X POST http://localhost:8080/api/ingest/dart` 수동 수집 동작
- [ ] 종목 상세 페이지에서 DART 공시 이력 확인
- [ ] 동일 보고서 중복 수집 시 DB에 중복 행 없음 확인
- [ ] DART API 키 미설정 시 기존 공공데이터포털 API 정상 동작 확인 (격리 검증)
- [ ] plan.md의 API 설계(`dartDisclosures`, `latestDartDisclosure`)와 실제 구현 일치 확인
