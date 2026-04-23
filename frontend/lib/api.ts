export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080/api";

export type Market = "KOSPI" | "KOSDAQ" | "OVERSEAS" | "UNKNOWN";
export type AssetClass =
  | "DOMESTIC_EQUITY"
  | "OVERSEAS_EQUITY"
  | "DOMESTIC_BOND"
  | "OVERSEAS_BOND"
  | "ALTERNATIVE"
  | "CASH"
  | "UNKNOWN";

export interface HoldingResponse {
  stockCode: string;
  stockName: string;
  market: Market;
  fiscalYear: number;
  shares: number;
  marketValue: string;
  ownershipRatio: string;
  assetClass: AssetClass;
  yearOverYearChangeRatio: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface HoldingTrendPoint {
  fiscalYear: number;
  shares: number;
  marketValue: string;
  ownershipRatio: string;
}

export interface HoldingTrendResponse {
  stockCode: string;
  stockName: string;
  market: Market;
  points: HoldingTrendPoint[];
}

export interface AssetClassSummary {
  assetClass: AssetClass;
  count: number;
  totalValue: string;
}

export interface HoldingSummaryResponse {
  fiscalYear: number;
  assetClasses: AssetClassSummary[];
  topHoldings: HoldingResponse[];
}

export interface ListHoldingsParams {
  year: number;
  market?: Market;
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}

async function getJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { Accept: "application/json", ...(init?.headers ?? {}) },
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`API ${path} failed: ${res.status}`);
  }
  return (await res.json()) as T;
}

export function listHoldings(params: ListHoldingsParams) {
  const qs = new URLSearchParams();
  qs.set("year", String(params.year));
  if (params.market) qs.set("market", params.market);
  if (params.q) qs.set("q", params.q);
  if (params.page !== undefined) qs.set("page", String(params.page));
  if (params.size !== undefined) qs.set("size", String(params.size));
  if (params.sort) qs.set("sort", params.sort);
  return getJson<PageResponse<HoldingResponse>>(`/holdings?${qs.toString()}`);
}

export function getHoldingTrend(stockCode: string) {
  return getJson<HoldingTrendResponse>(`/holdings/${stockCode}/trend`);
}

export function getSummary(year: number) {
  return getJson<HoldingSummaryResponse>(`/holdings/summary?year=${year}`);
}

export function formatKRW(value: string | number): string {
  const num = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(num)) return "-";
  const EOK = 100_000_000;
  if (Math.abs(num) >= EOK) {
    return `${(num / EOK).toLocaleString("ko-KR", { maximumFractionDigits: 1 })}억`;
  }
  return num.toLocaleString("ko-KR");
}

export function formatPercent(value: string | number | null): string {
  if (value === null || value === undefined) return "-";
  const num = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(num)) return "-";
  return `${(num * 100).toFixed(2)}%`;
}
