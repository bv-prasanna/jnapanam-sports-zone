import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Jnapanam Sports Zone | Holalkere", template: "%s | Jnapanam Sports Zone" },
  description: "Sports, fitness, coaching and community for every age in Holalkere.",
  other: {
    "codex-preview": "development",
  },
  icons: {
    icon: "/images/jnapanam-logo.png",
    shortcut: "/images/jnapanam-logo.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">{children}</body>
    </html>
  );
}
