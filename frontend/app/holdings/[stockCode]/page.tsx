"use client";

import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { getHoldingTrend, formatKRW, formatPercent } from "@/lib/api";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
  Legend,
} from "recharts";

export default function HoldingDetailPage({
  params,
}: {
  params: Promise<{ stockCode: string }>;
}) {
  const { stockCode } = use(params);
  const { data, isLoading, error } = useQuery({
    queryKey: ["trend", stockCode],
    queryFn: () => getHoldingTrend(stockCode),
  });

  if (isLoading) return <p className="p-10 text-slate-500">불러오는 중…</p>;
  if (error || !data)
    return (
      <p className="p-10 text-red-600">
        종목 데이터를 가져올 수 없습니다 ({stockCode}).
      </p>
    );

  const chartData = data.points.map((p) => ({
    year: String(p.fiscalYear),
    shares: Number(p.shares),
    marketValue: Number(p.marketValue) / 100_000_000,
    ownershipRatio: Number(p.ownershipRatio),
  }));
  const latest = data.points[data.points.length - 1];

  return (
    <div className="mx-auto max-w-5xl px-6 py-10 space-y-8">
      <section>
        <p className="text-xs text-slate-400">{data.market} · {data.stockCode}</p>
        <h1 className="text-3xl font-bold">{data.stockName}</h1>
      </section>

      {latest && (
        <section className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card label={`${latest.fiscalYear}년 보유수량`} value={Number(latest.shares).toLocaleString()} />
          <Card label="평가금액" value={formatKRW(latest.marketValue)} />
          <Card label="지분율" value={formatPercent(Number(latest.ownershipRatio) / 100)} />
          <Card label="기록된 연도 수" value={`${data.points.length}개`} />
        </section>
      )}

      <section>
        <h2 className="text-xl font-semibold mb-3">연도별 추이 (평가금액 · 단위: 억원)</h2>
        <div className="h-80 rounded-lg border bg-white p-4">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="year" />
              <YAxis />
              <Tooltip formatter={(v) => `${Number(v).toFixed(1)}억원`} />
              <Legend />
              <Line type="monotone" dataKey="marketValue" name="평가금액(억)" stroke="#2563eb" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-3">상세 기록</h2>
        <div className="overflow-x-auto rounded-lg border bg-white">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-100">
              <tr>
                <th className="px-4 py-2 text-left">연도</th>
                <th className="px-4 py-2 text-right">보유수량</th>
                <th className="px-4 py-2 text-right">평가금액</th>
                <th className="px-4 py-2 text-right">지분율</th>
              </tr>
            </thead>
            <tbody>
              {[...data.points].reverse().map((p) => (
                <tr key={p.fiscalYear} className="border-t">
                  <td className="px-4 py-2">{p.fiscalYear}</td>
                  <td className="px-4 py-2 text-right">{Number(p.shares).toLocaleString()}</td>
                  <td className="px-4 py-2 text-right">{formatKRW(p.marketValue)}</td>
                  <td className="px-4 py-2 text-right">{formatPercent(Number(p.ownershipRatio) / 100)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-white p-4">
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-1 text-xl font-semibold">{value}</div>
    </div>
  );
}
