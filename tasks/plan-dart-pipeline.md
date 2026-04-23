# Plan: DART 대량보유보고서 수집 파이프라인

## 상태
- 작성일: 2026-04-23
- 최종 수정: 2026-04-23
- 상태: Draft
- 참조 PRD: `tasks/prd-dart-pipeline.md`

---

## 요구사항 요약

DART OpenAPI를 통해 국민연금의 대량보유보고서를 수집하고, 기존 공공데이터포털 연간 데이터와 함께 조회할 수 있도록 한다. 동일 종목에 양쪽 데이터가 존재하면 공공데이터포털 데이터를 우선하고 DART 데이터는 보조 표기한다. 5% 미만 보유 종목은 별도 표기한다. 수집은 수동 및 일 1회 자동으로 실행된다.

---

## 아키텍처 결정

### 레이어 구조

기존 프로젝트의 레이어 구조를 그대로 따른다.

```
Presentation (IngestionController, HoldingController)
    ↓
Application (DartIngestionService, HoldingQueryService)
    ↓
Domain (DartDisclosure — 신규 도메인)
    ↑
Infrastructure (DartDisclosureJpaEntity, DartDisclosureRepository, DartOpenApiClient)
```

### 주요 결정 사항

| 결정 | 선택한 방향 | 이유 |
|------|------------|------|
| DART 데이터 저장 위치 | 별도 테이블 (`dart_disclosures`) | 기존 `holdings`는 `(stock_code, fiscal_year)` 유니크 키 구조라 날짜 기반인 DART 데이터와 충돌 발생. 도메인 성격 자체가 다름 |
| 기존 Holding 도메인 수정 여부 | 수정 없음 | DART 데이터는 보조 정보이므로 핵심 도메인을 오염시키지 않음 |
| 데이터 우선순위 적용 위치 | API 응답 조립 시 (Application 레이어) | DB에 중복 저장 없이 조회 시점에 병합 |
| 자동 수집 구현 방식 | Spring `@Scheduled` | 외부 스케줄러 없이 단순하게 구현 가능. 향후 배치 프레임워크로 전환 가능 |
| DART 국민연금 식별자 | 고유번호 `00131747` 고정 | 국민연금은 단일 기관이므로 설정값으로 관리 |
| 5% 미만 종목 처리 | `below_five_percent` 플래그 컬럼으로 저장 | 응답 시 별도 표기를 위한 구분 값 필요 |

---

## 데이터 모델

### 신규 도메인: DartDisclosure

DART 대량보유보고서 1건을 표현하는 도메인 객체.

```
DartDisclosure
- rceptNo: String          — DART 접수번호 (전역 유니크 ID)
- stockCode: String        — 종목코드 (DART는 실제 코드 제공)
- stockName: String        — 종목명
- rceptDate: LocalDate     — 공시 기준일
- reportType: ReportType   — NEW(신규) / CHANGE(변동)
- ownershipRatio: BigDecimal — 보유비율 (%)
- shares: Long             — 보유주식수
- belowFivePercent: Boolean — 5% 미만 여부
```

### 신규 JPA 엔티티: DartDisclosureJpaEntity → 테이블 `dart_disclosures`

```
dart_disclosures
- id: BIGINT (PK, auto increment)
- rcept_no: VARCHAR(20) UNIQUE NOT NULL  ← 중복 수집 방지 키
- stock_code: VARCHAR(32) NOT NULL
- stock_name: VARCHAR(128) NOT NULL
- rcept_date: DATE NOT NULL
- report_type: VARCHAR(16) NOT NULL      ← 'NEW' | 'CHANGE'
- ownership_ratio: DECIMAL(10,6) NOT NULL
- shares: BIGINT NOT NULL
- below_five_percent: BOOLEAN NOT NULL DEFAULT FALSE
- created_at: TIMESTAMP NOT NULL
```

인덱스:
- `idx_dart_stock_code` on `stock_code`
- `idx_dart_rcept_date` on `rcept_date`

### 기존 모델 변경 없음

`Holding`, `HoldingJpaEntity`, `Stock`, `StockJpaEntity` 는 수정하지 않는다.

---

## 신규 파일 구조

CLAUDE.md의 "신규 기관 추가 패턴"을 따른다.

```
ingest/
├── DartOpenApiProperties.kt     — API 키, base URL 설정 바인딩
├── DartOpenApiClient.kt         — DART API HTTP 호출
├── DartDataParser.kt            — API 응답 → DartDisclosure 도메인 변환
└── DartIngestionService.kt      — 수집 오케스트레이션 + 자동 스케줄러

holding/
├── domain/
│   └── DartDisclosure.kt        — 신규 도메인 객체
└── infrastructure/
    ├── DartDisclosureJpaEntity.kt
    └── DartDisclosureRepository.kt
```

---

## DART API 연동

### 사용 엔드포인트

```
GET https://opendart.fss.or.kr/api/majorstock.json
  ?crtfc_key={API_KEY}
  &corp_code=00131747        ← 국민연금 고유번호
  &bgn_de={YYYYMMDD}
  &end_de={YYYYMMDD}
```

응답 주요 필드:

| DART 필드명 | 매핑 대상 | 비고 |
|---|---|---|
| `rcept_no` | `rceptNo` | 유니크 ID |
| `stock_code` | `stockCode` | 실제 종목코드 제공 |
| `corp_name` | `stockName` | |
| `rcept_dt` | `rceptDate` | YYYYMMDD → LocalDate |
| `report_tp` | `reportType` | 신규/변동 |
| `number_of_voting_right` | `shares` | |
| `holding_ratio` | `ownershipRatio` | |

`holding_ratio < 5.0` 이면 `belowFivePercent = true`.

### 수집 범위

최초 수집: 과거 전체 기간 (DART 서비스 개시 이후)  
이후 자동 수집: 전일 기준 1일치  
중복 방지: `rcept_no` UNIQUE 제약으로 처리 (upsert 불필요, insert 무시)

---

## API 설계

### 수집 트리거 (기존 IngestionController 확장)

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/ingest/dart` | DART 수동 수집 실행. 쿼리 파라미터 없음 (내부적으로 미수집 구간 자동 계산) |

응답: 기존 `IngestionLogJpaEntity` 응답 형태 재사용 (`source = "dart.fss.or.kr"`).

### 기존 API 응답 확장

#### `GET /api/holdings/{stockCode}/trend`

현재 응답에 `dartDisclosures` 배열 추가:

```
HoldingTrendResponse
  - trend: List<YearlyHolding>     ← 기존 (공공데이터포털)
  - dartDisclosures: List<DartDisclosureItem>  ← 신규
      - rceptDate: String
      - reportType: String         ← "NEW" | "CHANGE"
      - ownershipRatio: BigDecimal
      - shares: Long
      - belowFivePercent: Boolean
      - source: "DART"
```

#### `GET /api/holdings/{stockCode}` (단건)

응답에 `latestDartDisclosure` 필드 추가 (최신 공시 1건, nullable):

```
HoldingResponse
  - (기존 필드 유지)
  - latestDartDisclosure: DartDisclosureItem?
```

#### `GET /api/holdings` (목록)

변경 없음. 목록에서는 DART 데이터 노출 안 함 (성능·복잡도 고려).

---

## 스케줄러

`DartIngestionService`에 `@Scheduled(cron = "0 0 7 * * *")` 적용.  
매일 오전 7시 전일 공시분 자동 수집.  
DART API 장애 시 예외를 catch하여 로그만 기록하고 서비스 영향 없도록 처리.

---

## 설정 추가

`application.yml` / `application-local.yml`:

```yaml
dart:
  openapi:
    base-url: https://opendart.fss.or.kr/api
    api-key: ${DART_API_KEY}
    corp-code: "00131747"   # 국민연금
```

`DartOpenApiProperties.kt`가 이 값을 바인딩.

---

## 프론트엔드 변경 (백엔드 API 확장 후)

| 위치 | 변경 내용 |
|---|---|
| `holdings/[stockCode]/page.tsx` | 트렌드 차트 아래 DART 공시 이력 테이블 추가 |
| `holdings/[stockCode]/page.tsx` | 5% 미만 항목에 별도 배지 표시 |
| `holdings/page.tsx` | 변경 없음 |
| `page.tsx` (홈) | 변경 없음 |

---

## 제외한 대안

| 대안 | 제외 이유 |
|---|---|
| `holdings` 테이블에 `source` 컬럼 추가하여 통합 | DART는 연도가 아닌 날짜 기반 데이터. 기존 유니크 제약 변경 시 마이그레이션 복잡도 증가 |
| WebFlux 비동기 수집 | 현재 프로젝트가 MVC 기반. 수집 빈도(일 1회)상 비동기 불필요 |
| Quartz 스케줄러 | Spring `@Scheduled`로 충분한 단순 주기 작업 |
| DART Webhook/SSE | DART API가 push 방식 미지원 |
