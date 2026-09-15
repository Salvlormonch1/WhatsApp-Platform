import { cn } from "@/lib/utils";

interface CardProps {
  className?: string;
  children: React.ReactNode;
}

export function Card({ className, children }: CardProps) {
  return (
    <div className={cn("rounded-2xl bg-gray-900/50 border border-gray-800/60 p-6", className)}>
      {children}
    </div>
  );
}

export function CardHeader({ className, children }: CardProps) {
  return (
    <div className={cn("flex items-center justify-between mb-5", className)}>
      {children}
    </div>
  );
}

export function CardTitle({ className, children }: CardProps) {
  return (
    <h2 className={cn("text-base font-semibold text-gray-100", className)}>
      {children}
    </h2>
  );
}

interface StatCardProps {
  label: string;
  value: string | number;
  subtext?: string;
  icon: React.ReactNode;
  trend?: { value: number; label: string };
  color?: "indigo" | "emerald" | "amber" | "red" | "violet";
}

const colorMap = {
  indigo:  { bg: "bg-indigo-500/10",  icon: "text-indigo-400",  ring: "ring-indigo-500/20" },
  emerald: { bg: "bg-emerald-500/10", icon: "text-emerald-400", ring: "ring-emerald-500/20" },
  amber:   { bg: "bg-amber-500/10",   icon: "text-amber-400",   ring: "ring-amber-500/20" },
  red:     { bg: "bg-red-500/10",     icon: "text-red-400",     ring: "ring-red-500/20" },
  violet:  { bg: "bg-violet-500/10",  icon: "text-violet-400",  ring: "ring-violet-500/20" },
};

export function StatCard({ label, value, subtext, icon, trend, color = "indigo" }: StatCardProps) {
  const c = colorMap[color];
  return (
    <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5 flex items-start gap-4 hover:border-gray-700/60 transition-colors group">
      <div className={cn("p-2.5 rounded-xl ring-1 flex-shrink-0 transition-all", c.bg, c.ring, c.icon)}>
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs text-gray-500 font-medium uppercase tracking-wider">{label}</p>
        <p className="text-2xl font-bold text-white mt-0.5 leading-none">{value}</p>
        {subtext && <p className="text-xs text-gray-500 mt-1">{subtext}</p>}
        {trend && (
          <p className={cn("text-xs mt-1 font-medium", trend.value >= 0 ? "text-emerald-400" : "text-red-400")}>
            {trend.value >= 0 ? "▲" : "▼"} {Math.abs(trend.value)}% {trend.label}
          </p>
        )}
      </div>
    </div>
  );
}
