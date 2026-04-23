"use client";

import Link from "next/link";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { listHoldings, formatKRW, formatPercent, type Market } from "@/lib/api";
import { useState } from "react";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 6 }, (_, i) => CURRENT_YEAR - 1 - i);
const MARKETS: { label: string; value: Market | "ALL" }[] = [
  { label: "전체", value: "ALL" },
  { label: "KOSPI", value: "KOSPI" },
  { label: "KOSDAQ", value: "KOSDAQ" },
  { label: "해외", value: "OVERSEAS" },
];

export default function HoldingsPage() {
  const [year, setYear] = useState<number>(YEARS[0]);
  const [market, setMarket] = useState<Market | "ALL">("ALL");
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [page, setPage] = useState(0);

  const { data, isFetching, error } = useQuery({
    queryKey: ["holdings", year, market, submittedQuery, page],
    queryFn: () =>
      listHoldings({
        year,
        market: market === "ALL" ? undefined : market,
        q: submittedQuery || undefined,
        page,
        size: 20,
        sort: "marketValue,desc",
      }),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="mx-auto max-w-6xl px-6 py-10 space-y-6">
      <h1 className="text-2xl font-bold">종목별 보유 현황</h1>

      <form
        className="flex flex-wrap gap-3 items-end"
        onSubmit={(e) => {
          e.preventDefault();
          setPage(0);
          setSubmittedQuery(query.trim());
        }}
      >
        <div>
          <label className="block text-xs text-slate-500 mb-1">연도</label>
          <select
            className="rounded border px-3 py-1.5 text-sm"
            value={year}
            onChange={(e) => {
              setPage(0);
              setYear(Number(e.target.value));
            }}
          >
            {YEARS.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-slate-500 mb-1">시장</label>
          <select
            className="rounded border px-3 py-1.5 text-sm"
            value={market}
            onChange={(e) => {
              setPage(0);
              setMarket(e.target.value as Market | "ALL");
            }}
          >
            {MARKETS.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
        </div>
        <div className="flex-1 min-w-[220px]">
          <label className="block text-xs text-slate-500 mb-1">검색 (종목명/코드)</label>
          <input
            className="w-full rounded border px-3 py-1.5 text-sm"
            placeholder="예: 삼성전자"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
        <button className="rounded bg-slate-900 text-white text-sm px-4 py-1.5">검색</button>
      </form>

      {error && <p className="text-red-600">API 호출 실패.</p>}

      <div className="overflow-x-auto rounded-lg border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-100">
            <tr>
              <th className="px-4 py-2 text-left">종목명</th>
              <th className="px-4 py-2 text-left">시장</th>
              <th className="px-4 py-2 text-right">보유수량</th>
              <th className="px-4 py-2 text-right">평가금액</th>
              <th className="px-4 py-2 text-right">지분율</th>
              <th className="px-4 py-2 text-right">전년 대비</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((h) => (
              <tr key={h.stockCode} className="border-t hover:bg-slate-50">
                <td className="px-4 py-2">
                  <Link className="text-blue-600 hover:underline" href={`/holdings/${h.stockCode}`}>
                    {h.stockName}
                  </Link>
                  <span className="ml-2 text-xs text-slate-400">{h.stockCode}</span>
                </td>
                <td className="px-4 py-2">{h.market}</td>
                <td className="px-4 py-2 text-right">{h.shares.toLocaleString()}</td>
                <td className="px-4 py-2 text-right">{formatKRW(h.marketValue)}</td>
                <td className="px-4 py-2 text-right">{formatPercent(Number(h.ownershipRatio) / 100)}</td>
                <td className={`px-4 py-2 text-right ${
                  h.yearOverYearChangeRatio === null
                    ? "text-slate-400"
                    : Number(h.yearOverYearChangeRatio) > 0
                      ? "text-red-600"
                      : Number(h.yearOverYearChangeRatio) < 0
                        ? "text-blue-600"
                        : ""
                }`}>
                  {formatPercent(h.yearOverYearChangeRatio)}
                </td>
              </tr>
            ))}
            {!isFetching && (data?.content.length ?? 0) === 0 && (
              <tr>
                <td className="px-4 py-6 text-center text-slate-500" colSpan={6}>
                  결과가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-3 text-sm">
          <button
            className="rounded border px-3 py-1 disabled:opacity-50"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >이전</button>
          <span>{page + 1} / {data.totalPages}</span>
          <button
            className="rounded border px-3 py-1 disabled:opacity-50"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >다음</button>
        </div>
      )}
    </div>
  );
}
