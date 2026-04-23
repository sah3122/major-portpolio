import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "국민연금 포트폴리오",
  description: "국민연금기금의 종목별 보유 현황과 변동 추이를 조회하세요.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col bg-slate-50 text-slate-900">
        <header className="border-b bg-white">
          <div className="mx-auto max-w-6xl px-6 py-4 flex items-center gap-6">
            <Link href="/" className="font-bold text-lg">
              국민연금 포트폴리오
            </Link>
            <nav className="flex gap-4 text-sm text-slate-600">
              <Link href="/" className="hover:text-slate-900">홈</Link>
              <Link href="/holdings" className="hover:text-slate-900">종목 조회</Link>
            </nav>
          </div>
        </header>
        <main className="flex-1">
          <Providers>{children}</Providers>
        </main>
        <footer className="border-t bg-white text-xs text-slate-500">
          <div className="mx-auto max-w-6xl px-6 py-4">
            데이터 출처: 공공데이터포털 (국민연금공단)
          </div>
        </footer>
      </body>
    </html>
  );
}
