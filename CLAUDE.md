# Major Portfolio Viewer — CLAUDE.md

## 프로젝트 개요

**메이저 금융사(연기금·자산운용사 등)의 포트폴리오를 한눈에 비교·조회**할 수 있는 웹사이트.

여러 기관의 종목별 보유 현황, 연도별 변동 추이, 기관 간 공통 보유 종목 등을 통합 조회한다.

### 지원 기관 (확장 예정)

| 기관 | 데이터 출처 | 현황 |
|---|---|---|
| 국민연금공단 (NPS) | 공공데이터포털 `3070507` | ✅ 구현 완료 |
| 사학연금 | 공공데이터포털 | 🔲 예정 |
| 공무원연금 | 공공데이터포털 | 🔲 예정 |
| 한국투자공사 (KIC) | 공시 자료 | 🔲 예정 |
| 주요 자산운용사 | 공시 자료 | 🔲 예정 |

데이터 기준일: 매년 12월 31일 → 이듬해 9월경 공개 (기관마다 상이)

## 기술 스택

| 영역 | 기술 |
|---|---|
| 백엔드 | Spring Boot 4.0.3 + Kotlin 2.2, JPA, H2(로컬) / PostgreSQL(운영) |
| 프론트엔드 | Next.js 16 + React 19, TanStack Query/Table, Recharts, Tailwind CSS |
| 테스트 | Kotest 5.9.1 (DescribeSpec 스타일, Spring 컨텍스트 없는 단위 테스트) |
| 인프라 | Docker Compose (PostgreSQL) |

## 패키지 구조

```
major-portpolio/
├── backend/src/main/kotlin/dean/ai/nps/
│   ├── holding/
│   │   ├── domain/          # 순수 도메인 (Stock, Holding, Market, AssetClass)
│   │   ├── application/     # HoldingQueryService
│   │   ├── infrastructure/  # JPA 엔티티 + 리포지토리
│   │   └── presentation/    # HoldingController + DTO
│   ├── ingest/              # 수집 파이프라인 (기관별 Client·Parser·Service)
│   └── common/              # PageResponse
├── frontend/app/
│   ├── page.tsx             # 홈 (자산군 카드 + Top 10)
│   ├── holdings/page.tsx    # 종목 리스트 (검색·필터·페이징)
│   └── holdings/[stockCode]/page.tsx  # 종목 상세 + 연도별 추이 차트
├── docker-compose.yml
└── .claude/launch.json      # dev 서버 기동 설정
```

### 신규 기관 추가 패턴

새 기관을 추가할 때는 아래 3개 파일만 추가하고, 기존 도메인·컨트롤러는 재사용한다.

```
ingest/
├── {기관명}OpenApiClient.kt   # 외부 API 호출
├── {기관명}DataParser.kt      # 응답 → 도메인 매핑
└── {기관명}IngestionService.kt
```

`Holding` 도메인에 `institution: String` 필드 추가로 기관 구분.

## 주요 설정

- **서비스키**: `backend/src/main/resources/application-local.yml` (git 제외)
- **기본 프로파일**: `local` (H2 인메모리 DB 자동 사용)
- **NPS API 경로**: `3070507/v1/uddi:cc757223-fdc0-45b2-a617-dcbecec3fe1f`

## 로컬 실행

```bash
# 백엔드 (포트 8080)
cd backend && ./gradlew bootRun

# 프론트엔드 (포트 3000)
cd frontend && npm run dev

# NPS 2024년 데이터 수집
curl -X POST "http://localhost:8080/api/ingest/run?year=2024"
```

## 테스트 규칙

- 기능 추가 또는 수정 시 반드시 Kotest 테스트를 함께 작성한다.
- 도메인 단위 테스트는 Spring 컨텍스트 없이 순수 단위 테스트로 작성한다.
- 테스트 스타일: `DescribeSpec`

```bash
cd backend && ./gradlew test
```

## REST API

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/holdings?year=2024&q=삼성&market=KOSPI` | 종목 목록 (페이징) |
| GET | `/api/holdings/{stockCode}?year=2024` | 종목 단건 조회 |
| GET | `/api/holdings/{stockCode}/trend` | 연도별 추이 |
| GET | `/api/holdings/summary?year=2024` | 자산군 요약 + Top 10 |
| POST | `/api/ingest/run?year=2024` | 공공데이터포털에서 수집 |

> 기관 파라미터(`institution=NPS` 등)는 다기관 지원 시 추가 예정.

## 데이터 특성

- 공공데이터포털 API는 **연도 필터 미지원** → 전체 1,200건 일괄 수집 후 `fiscalYear`로 태깅
- `종목코드` 미제공 → `종목명` slug 처리해 코드로 사용
- `평가액(억 원)` 단위 → KRW 변환 시 × 100,000,000 적용
- 2025년 데이터는 2026년 9월 이후 공개 예정
