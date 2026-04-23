# NPS Portfolio Viewer

국민연금기금의 **종목별 보유 현황**과 **연도별 변동 추이**를 조회할 수 있는 웹사이트.

## 구성

- `backend/` — Spring Boot 4.0.3 + Kotlin 2.2 + JPA (PostgreSQL / H2)
- `frontend/` — Next.js 16 + React 19 + Tailwind + TanStack Query/Table + Recharts
- `docker-compose.yml` — 로컬 PostgreSQL

## 사전 준비

1. [공공데이터포털](https://www.data.go.kr)에서 "국민연금공단_국내주식 투자정보" 활용 신청 → 서비스키 발급
2. 환경 변수 설정:
   ```bash
   export NPS_SERVICE_KEY="..."
   ```

## 로컬 실행

### 백엔드 (기본 H2 프로파일 `local`)

```bash
cd backend
./gradlew bootRun
```

기본 포트 `8080`. H2 콘솔은 http://localhost:8080/h2-console.

### PostgreSQL + prod 프로파일로 실행하려면

```bash
docker compose up -d
cd backend
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

### 프론트엔드

```bash
cd frontend
npm run dev
```

http://localhost:3000 접속.

## 데이터 적재

```bash
# 2025년 공시 데이터 수동 수집
curl -X POST "http://localhost:8080/api/ingest/run?year=2025"
```

공공데이터포털 API는 연 1회 갱신되므로, 시기별 추이 차트 품질을 높이려면 과거 연도를 순차 호출해 누적 적재한다.

## 주요 API

| Method | Path |
|---|---|
| GET | `/api/holdings?year=2025&q=삼성&market=KOSPI` |
| GET | `/api/holdings/{stockCode}?year=2025` |
| GET | `/api/holdings/{stockCode}/trend` |
| GET | `/api/holdings/summary?year=2025` |
| POST | `/api/ingest/run?year=2025` |

## 테스트

```bash
cd backend && ./gradlew test
```
