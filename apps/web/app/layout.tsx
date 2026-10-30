import type { Metadata } from "next";
import "./globals.css";
import { AppProvider } from "@/components/app-provider";

export const metadata: Metadata = {
  title: { default: "Jnapanam Sports Zone | Holalkere", template: "%s | Jnapanam Sports Zone" },
  description: "Sports, fitness, coaching and community for every age in Holalkere.",
  other: {
    "codex-preview": "development",
  },
  icons: {
    icon: "/images/jnapanam-logo.jpg",
    shortcut: "/images/jnapanam-logo.jpg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased"><AppProvider>{children}</AppProvider></body>
    </html>
  );
}
