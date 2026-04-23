"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { formatKRW, formatPercent, getSummary } from "@/lib/api";
import { useState } from "react";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 6 }, (_, i) => CURRENT_YEAR - 1 - i);

export default function HomePage() {
  const [year, setYear] = useState<number>(YEARS[0]);
  const { data, isLoading, error } = useQuery({
    queryKey: ["summary", year],
    queryFn: () => getSummary(year),
  });

  return (
    <div className="mx-auto max-w-6xl px-6 py-10 space-y-8">
      <section>
        <h1 className="text-3xl font-bold">국민연금 보유 포트폴리오</h1>
        <p className="mt-2 text-slate-600">
          공공데이터포털의 국민연금 공시 데이터를 기반으로 종목별 보유 현황과 연도별 변동 추이를 제공합니다.
        </p>
      </section>

      <section className="flex items-center gap-3">
        <label className="text-sm text-slate-600">연도</label>
        <select
          className="rounded border px-3 py-1.5 text-sm"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
        >
          {YEARS.map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>
      </section>

      {isLoading && <p className="text-slate-500">불러오는 중…</p>}
      {error && (
        <p className="text-red-600">
          데이터를 가져오지 못했습니다. 백엔드가 실행 중인지 확인하세요.
        </p>
      )}

      {data && (
        <>
          <section>
            <h2 className="text-xl font-semibold mb-3">자산군 구성</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {data.assetClasses.map((ac) => (
                <div key={ac.assetClass} className="rounded-lg border bg-white p-4">
                  <div className="text-sm text-slate-500">{ac.assetClass}</div>
                  <div className="mt-1 text-2xl font-bold">{formatKRW(ac.totalValue)}</div>
                  <div className="text-xs text-slate-500 mt-1">{ac.count.toLocaleString()} 종목</div>
                </div>
              ))}
              {data.assetClasses.length === 0 && (
                <p className="text-slate-500 col-span-4">
                  {year}년 데이터가 없습니다. 관리자가 <code>POST /api/ingest/run?year={year}</code>을 실행해주세요.
                </p>
              )}
            </div>
          </section>

          <section>
            <h2 className="text-xl font-semibold mb-3">보유금액 상위 10종목</h2>
            <div className="overflow-x-auto rounded-lg border bg-white">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-100">
                  <tr>
                    <th className="px-4 py-2 text-left">종목명</th>
                    <th className="px-4 py-2 text-left">시장</th>
                    <th className="px-4 py-2 text-right">평가금액</th>
                    <th className="px-4 py-2 text-right">지분율</th>
                  </tr>
                </thead>
                <tbody>
                  {data.topHoldings.map((h) => (
                    <tr key={h.stockCode} className="border-t">
                      <td className="px-4 py-2">
                        <Link className="text-blue-600 hover:underline" href={`/holdings/${h.stockCode}`}>
                          {h.stockName}
                        </Link>
                        <span className="ml-2 text-xs text-slate-400">{h.stockCode}</span>
                      </td>
                      <td className="px-4 py-2">{h.market}</td>
                      <td className="px-4 py-2 text-right">{formatKRW(h.marketValue)}</td>
                      <td className="px-4 py-2 text-right">{formatPercent(Number(h.ownershipRatio) / 100)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
