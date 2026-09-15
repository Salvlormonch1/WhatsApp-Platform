"use client";

import { useReports, useLatestReport } from "@/lib/api/hooks";
import { PageLoader, EmptyState, PageHeader, Button } from "@/components/ui/Shared";
import { StatCard } from "@/components/ui/Card";
import { formatDate } from "@/lib/utils";
import {
  BarChart3, CalendarDays, Users, MessageSquare,
  Bot, TrendingDown, RefreshCw, CheckCircle2, XCircle,
} from "lucide-react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Cell,
} from "recharts";
import { reportsApi } from "@/lib/api/endpoints";
import { useQueryClient, useMutation } from "@tanstack/react-query";

const TOOLTIP_STYLE = {
  backgroundColor: "#1e293b",
  border: "1px solid rgba(71,85,105,0.4)",
  borderRadius: "12px",
  color: "#f1f5f9",
  fontSize: "12px",
};

export default function ReportsPage() {
  const { data: latest, isLoading: latestLoading } = useLatestReport();
  const { data: reports } = useReports();
  const qc = useQueryClient();

  const { mutate: generateReport, isPending: generating } = useMutation({
    mutationFn: reportsApi.generate,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["report-latest"] });
      qc.invalidateQueries({ queryKey: ["reports"] });
    },
  });

  if (latestLoading) return <PageLoader />;

  const apptData = latest ? [
    { name: "Confirmadas", value: latest.data.appointments.confirmed, color: "#10b981" },
    { name: "Completadas", value: latest.data.appointments.completed, color: "#3b82f6" },
    { name: "Canceladas",  value: latest.data.appointments.cancelled, color: "#ef4444" },
    { name: "No asistió",  value: latest.data.appointments.no_show,   color: "#6b7280" },
  ] : [];

  return (
    <div className="fade-in">
      <PageHeader
        title="Reportes"
        subtitle="Análisis de rendimiento semanal"
        action={
          <Button variant="primary" onClick={() => generateReport()} disabled={generating}>
            <RefreshCw className={`w-4 h-4 ${generating ? "animate-spin" : ""}`} />
            {generating ? "Generando…" : "Generar reporte"}
          </Button>
        }
      />

      {!latest ? (
        <EmptyState
          icon={<BarChart3 className="w-7 h-7" />}
          title="Sin reportes"
          description="Genera tu primer reporte para ver el análisis de rendimiento"
          action={
            <Button variant="primary" onClick={() => generateReport()} disabled={generating}>
              <RefreshCw className={`w-4 h-4 ${generating ? "animate-spin" : ""}`} />
              Generar primer reporte
            </Button>
          }
        />
      ) : (
        <>
          {/* Period header */}
          <div className="mb-6 px-4 py-3 rounded-xl bg-gray-900/50 border border-gray-800/60 flex items-center justify-between">
            <div>
              <p className="text-xs text-gray-500">Período del reporte</p>
              <p className="text-sm font-semibold text-gray-200">
                {formatDate(latest.periodStart)} — {formatDate(latest.periodEnd)}
              </p>
            </div>
            {latest.sentAt && (
              <div className="flex items-center gap-1.5 text-xs text-emerald-400">
                <CheckCircle2 className="w-3.5 h-3.5" />
                Enviado por email
              </div>
            )}
          </div>

          {/* KPI grid */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            <StatCard
              label="Total reservas"
              value={latest.data.appointments.total}
              subtext={`${latest.data.appointments.confirmed} confirmadas`}
              icon={<CalendarDays className="w-5 h-5" />}
              color="indigo"
            />
            <StatCard
              label="Clientes nuevos"
              value={latest.data.customers.new}
              subtext="Desde WhatsApp"
              icon={<Users className="w-5 h-5" />}
              color="emerald"
            />
            <StatCard
              label="Conversaciones"
              value={latest.data.conversations.total}
              subtext={`${latest.data.conversations.escalated} escaladas`}
              icon={<MessageSquare className="w-5 h-5" />}
              color="violet"
            />
            <StatCard
              label="Tasa escalación"
              value={`${latest.data.conversations.escalation_rate}%`}
              subtext="Conversaciones escaladas a humano"
              icon={<TrendingDown className="w-5 h-5" />}
              color={parseFloat(latest.data.conversations.escalation_rate) > 20 ? "red" : "amber"}
            />
          </div>

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-8">
            {/* Appointment breakdown bar chart */}
            <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
              <h2 className="text-base font-semibold text-gray-100 mb-5">Distribución de reservas</h2>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={apptData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="name" tick={{ fill: "#6b7280", fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fill: "#6b7280", fontSize: 11 }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={TOOLTIP_STYLE} />
                  <Bar dataKey="value" radius={[6, 6, 0, 0]} name="Reservas">
                    {apptData.map((entry, i) => (
                      <Cell key={i} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Summary panel */}
            <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5 space-y-4">
              <h2 className="text-base font-semibold text-gray-100">Resumen IA</h2>

              {latest.data.top_service && (
                <div className="flex items-start gap-3 p-3 rounded-xl bg-indigo-500/5 border border-indigo-500/10">
                  <div className="p-1.5 rounded-lg bg-indigo-500/10">
                    <CheckCircle2 className="w-4 h-4 text-indigo-400" />
                  </div>
                  <div>
                    <p className="text-xs text-gray-500">Servicio más solicitado</p>
                    <p className="text-sm font-semibold text-gray-200">{latest.data.top_service}</p>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                {[
                  {
                    label: "Gestionadas por IA",
                    value: latest.data.conversations.total - latest.data.conversations.escalated,
                    icon: <Bot className="w-4 h-4 text-emerald-400" />,
                    color: "bg-emerald-500/10 border-emerald-500/10",
                  },
                  {
                    label: "Escaladas a humano",
                    value: latest.data.conversations.escalated,
                    icon: <XCircle className="w-4 h-4 text-red-400" />,
                    color: "bg-red-500/10 border-red-500/10",
                  },
                ].map(({ label, value, icon, color }) => (
                  <div key={label} className={`p-3 rounded-xl border ${color}`}>
                    {icon}
                    <p className="text-xl font-bold text-white mt-1">{value}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{label}</p>
                  </div>
                ))}
              </div>

              {/* AI resolution rate bar */}
              <div>
                <div className="flex justify-between mb-1.5">
                  <span className="text-xs text-gray-500">Tasa de resolución IA</span>
                  <span className="text-xs font-semibold text-emerald-400">
                    {(100 - parseFloat(latest.data.conversations.escalation_rate)).toFixed(1)}%
                  </span>
                </div>
                <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-emerald-500 to-green-400 rounded-full transition-all duration-700"
                    style={{ width: `${100 - parseFloat(latest.data.conversations.escalation_rate)}%` }}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Historical reports */}
          {reports && reports.length > 1 && (
            <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
              <h2 className="text-base font-semibold text-gray-100 mb-4">Reportes anteriores</h2>
              <div className="space-y-2">
                {reports.slice(0, 8).map((r) => (
                  <div key={r.id} className="flex items-center justify-between px-4 py-3 rounded-xl hover:bg-gray-800/40 transition-colors">
                    <div>
                      <p className="text-sm font-medium text-gray-200">
                        {formatDate(r.periodStart)} — {formatDate(r.periodEnd)}
                      </p>
                      <p className="text-xs text-gray-500">
                        {r.data.appointments.total} reservas · {r.data.conversations.total} conversaciones
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      {r.sentAt && (
                        <span className="text-xs text-emerald-400 flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />Enviado
                        </span>
                      )}
                      <span className="text-xs text-gray-500">{formatDate(r.createdAt)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
