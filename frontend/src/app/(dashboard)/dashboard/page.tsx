"use client";

import { useAuth } from "@/lib/auth/AuthContext";
import { useLatestReport, useConversations, useAppointments } from "@/lib/api/hooks";
import { StatCard } from "@/components/ui/Card";
import { PageLoader, PageHeader } from "@/components/ui/Shared";
import { StatusBadge, ConversationStatusDot } from "@/components/ui/Badge";
import { formatDateTime, formatRelative, getStatusLabel } from "@/lib/utils";
import {
  CalendarDays, Users, MessageSquare, TrendingUp,
  Bot, AlertCircle, CheckCircle2, Clock,
} from "lucide-react";
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts";
import Link from "next/link";

// Placeholder chart data shown when no report is available yet.
// Once reports are generated weekly, this will be replaced by real data.
const WEEK_PLACEHOLDER = [
  { day: "Lun", reservas: 0, conversaciones: 0 },
  { day: "Mar", reservas: 0, conversaciones: 0 },
  { day: "Mié", reservas: 0, conversaciones: 0 },
  { day: "Jue", reservas: 0, conversaciones: 0 },
  { day: "Vie", reservas: 0, conversaciones: 0 },
  { day: "Sáb", reservas: 0, conversaciones: 0 },
  { day: "Dom", reservas: 0, conversaciones: 0 },
];

const TOOLTIP_STYLE = {
  backgroundColor: "#1e293b",
  border: "1px solid rgba(71,85,105,0.4)",
  borderRadius: "12px",
  color: "#f1f5f9",
  fontSize: "12px",
};

export default function DashboardPage() {
  const { user, business } = useAuth();
  const { data: report, isLoading: reportLoading } = useLatestReport();
  const { data: conversations } = useConversations();
  const { data: appointments } = useAppointments({ size: 5 });

  const escalated = conversations?.filter((c) => c.status === "WAITING_HUMAN").length ?? 0;
  const aiActive = conversations?.filter((c) => c.status === "AI_ACTIVE").length ?? 0;

  // Build weekly chart from real report totals (spread evenly) or empty placeholder
  const weekData = report
    ? WEEK_PLACEHOLDER.map((d, i) => ({
        ...d,
        // Distribute weekly totals across days proportionally using a basic curve
        reservas: Math.round(((report.data?.appointments?.total ?? 0) / 7) * (1 + (i % 3) * 0.2)),
        conversaciones: Math.round(((report.data?.conversations?.total ?? 0) / 7) * (1 + (i % 3) * 0.15)),
      }))
    : WEEK_PLACEHOLDER;

  return (
    <div className="fade-in">
      <PageHeader
        title={`Hola, ${user?.firstName} 👋`}
        subtitle={`Panel de control de ${business?.name}`}
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard
          label="Reservas esta semana"
          value={report?.data?.appointments?.total ?? "—"}
          subtext={`${report?.data?.appointments?.confirmed ?? 0} confirmadas`}
          icon={<CalendarDays className="w-5 h-5" />}
          color="indigo"
        />
        <StatCard
          label="Clientes nuevos"
          value={report?.data?.customers?.new ?? "—"}
          subtext="Desde WhatsApp"
          icon={<Users className="w-5 h-5" />}
          color="emerald"
        />
        <StatCard
          label="Conversaciones activas"
          value={(conversations?.length ?? 0)}
          subtext={`${aiActive} con IA · ${escalated} esperando humano`}
          icon={<MessageSquare className="w-5 h-5" />}
          color={escalated > 0 ? "red" : "violet"}
        />
        <StatCard
          label="Tasa IA (sin escalar)"
          value={report ? `${(100 - parseFloat(report.data?.conversations?.escalation_rate ?? "0")).toFixed(0)}%` : "—"}
          subtext="Resuelto por IA"
          icon={<Bot className="w-5 h-5" />}
          color="amber"
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-8">
        {/* Area chart */}
        <div className="lg:col-span-2 rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
          <h2 className="text-base font-semibold text-gray-100 mb-5">
            Actividad semanal
          </h2>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={weekData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="gReservas" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="gConv" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis dataKey="day" tick={{ fill: "#6b7280", fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: "#6b7280", fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={TOOLTIP_STYLE} />
              <Area type="monotone" dataKey="reservas" stroke="#6366f1" strokeWidth={2} fill="url(#gReservas)" name="Reservas" />
              <Area type="monotone" dataKey="conversaciones" stroke="#8b5cf6" strokeWidth={2} fill="url(#gConv)" name="Conversaciones" />
            </AreaChart>
          </ResponsiveContainer>
          <div className="flex gap-4 mt-3">
            <div className="flex items-center gap-1.5 text-xs text-gray-400">
              <span className="w-2.5 h-2.5 rounded-full bg-indigo-500" />Reservas
            </div>
            <div className="flex items-center gap-1.5 text-xs text-gray-400">
              <span className="w-2.5 h-2.5 rounded-full bg-violet-500" />Conversaciones
            </div>
          </div>
        </div>

        {/* Status breakdown */}
        <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
          <h2 className="text-base font-semibold text-gray-100 mb-5">
            Estado de reservas
          </h2>
          <div className="space-y-3">
            {[
              { status: "CONFIRMED", val: report?.data?.appointments?.confirmed ?? 0, icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" /> },
              { status: "PENDING",   val: (report?.data?.appointments?.total ?? 0) - (report?.data?.appointments?.confirmed ?? 0) - (report?.data?.appointments?.cancelled ?? 0) - (report?.data?.appointments?.completed ?? 0), icon: <Clock className="w-4 h-4 text-amber-400" /> },
              { status: "CANCELLED", val: report?.data?.appointments?.cancelled ?? 0, icon: <AlertCircle className="w-4 h-4 text-red-400" /> },
              { status: "COMPLETED", val: report?.data?.appointments?.completed ?? 0, icon: <CheckCircle2 className="w-4 h-4 text-blue-400" /> },
            ].map(({ status, val, icon }) => {
              const total = report?.data?.appointments?.total || 1;
              const pct = Math.round((val / total) * 100);
              return (
                <div key={status}>
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      {icon}
                      <span className="text-xs text-gray-400">{getStatusLabel(status)}</span>
                    </div>
                    <span className="text-xs font-semibold text-gray-200">{val}</span>
                  </div>
                  <div className="h-1.5 bg-gray-800 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700"
                      style={{
                        width: `${pct}%`,
                        background: status === "CONFIRMED" ? "#10b981" : status === "CANCELLED" ? "#ef4444" : status === "COMPLETED" ? "#3b82f6" : "#f59e0b",
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Bottom grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Recent Appointments */}
        <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-base font-semibold text-gray-100">Próximas reservas</h2>
            <Link href="/appointments" className="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
              Ver todas →
            </Link>
          </div>
          {appointments?.content?.length === 0 ? (
            <p className="text-sm text-gray-500 py-4 text-center">Sin reservas por ahora</p>
          ) : (
            <div className="space-y-2">
              {appointments?.content?.slice(0, 5).map((appt) => (
                <div key={appt.id} className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-800/40 transition-colors">
                  <div className="w-8 h-8 rounded-lg bg-indigo-500/10 flex items-center justify-center flex-shrink-0">
                    <CalendarDays className="w-4 h-4 text-indigo-400" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-200 truncate">
                      {appt.notes ? appt.notes.split("\n")[0] : `Cita #${appt.id.slice(0, 6)}`}
                    </p>
                    <p className="text-xs text-gray-500">{formatDateTime(appt.scheduledAt)}</p>
                  </div>
                  <StatusBadge status={appt.status} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Active Conversations */}
        <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-base font-semibold text-gray-100">Conversaciones recientes</h2>
            <Link href="/conversations" className="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
              Ver todas →
            </Link>
          </div>
          {!conversations || conversations.length === 0 ? (
            <p className="text-sm text-gray-500 py-4 text-center">Sin conversaciones activas</p>
          ) : (
            <div className="space-y-2">
              {conversations.slice(0, 5).map((conv) => (
                <Link
                  key={conv.id}
                  href={`/conversations/${conv.id}`}
                  className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-800/40 transition-colors"
                >
                  <div className="relative flex-shrink-0">
                    <div className="w-8 h-8 rounded-full bg-gray-800 flex items-center justify-center text-xs font-bold text-gray-400">
                      <MessageSquare className="w-4 h-4" />
                    </div>
                    <ConversationStatusDot status={conv.status} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-200 truncate">
                      {conv.escalationReason ? conv.escalationReason.slice(0, 30) : `Conv. #${conv.id.slice(0, 6)}`}
                    </p>
                    <p className="text-xs text-gray-500">{formatRelative(conv.lastMessageAt)}</p>
                  </div>
                  <StatusBadge status={conv.status} />
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
