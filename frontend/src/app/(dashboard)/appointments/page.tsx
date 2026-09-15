"use client";

import { useState } from "react";
import { useAppointments, useCancelAppointment, useUpdateAppointmentStatus } from "@/lib/api/hooks";
import { PageLoader, EmptyState, PageHeader, Button } from "@/components/ui/Shared";
import { StatusBadge } from "@/components/ui/Badge";
import { formatDateTime } from "@/lib/utils";
import { CalendarDays, Plus, Search, Filter, Trash2, CheckCircle } from "lucide-react";
import type { AppointmentStatus } from "@/types";

const STATUS_FILTERS: { label: string; value: AppointmentStatus | undefined }[] = [
  { label: "Todos", value: undefined },
  { label: "Confirmados", value: "CONFIRMED" },
  { label: "Pendientes", value: "PENDING" },
  { label: "Completados", value: "COMPLETED" },
  { label: "Cancelados", value: "CANCELLED" },
];

export default function AppointmentsPage() {
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isLoading } = useAppointments({ status: statusFilter, page, size: 20 });
  const { mutate: cancelAppointment, isPending: cancelling } = useCancelAppointment();
  const { mutate: updateStatus, isPending: updating } = useUpdateAppointmentStatus();

  return (
    <div className="fade-in">
      <PageHeader
        title="Reservas"
        subtitle="Gestiona todas las citas de tu negocio"
        action={
          <Button variant="primary" className="gap-2">
            <Plus className="w-4 h-4" />
            Nueva reserva
          </Button>
        }
      />

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="flex gap-1 p-1 rounded-xl bg-gray-900/60 border border-gray-800/50">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.label}
              onClick={() => { setStatusFilter(f.value); setPage(0); }}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                statusFilter === f.value
                  ? "bg-indigo-600 text-white"
                  : "text-gray-400 hover:text-gray-200"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-800/60">
                <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Cliente</th>
                <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Servicio</th>
                <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha y hora</th>
                <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Canal</th>
                <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Estado</th>
                <th className="px-5 py-3.5" />
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12">
                    <PageLoader />
                  </td>
                </tr>
              ) : !data?.content?.length ? (
                <tr>
                  <td colSpan={6} className="py-12">
                    <EmptyState
                      icon={<CalendarDays className="w-7 h-7" />}
                      title="Sin reservas"
                      description="Las reservas aparecerán aquí cuando los clientes las realicen"
                    />
                  </td>
                </tr>
              ) : (
                data.content.map((appt) => (
                  <tr key={appt.id} className="border-b border-gray-800/30 hover:bg-gray-800/20 transition-colors group">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-gray-200 font-mono text-xs">{appt.customerId.slice(0, 8)}…</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <p className="text-gray-300 font-mono text-xs">{appt.serviceId.slice(0, 8)}…</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <p className="text-gray-300">{formatDateTime(appt.scheduledAt)}</p>
                      <p className="text-xs text-gray-500">{appt.durationMinutes} min</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="text-xs px-2 py-0.5 rounded-md bg-gray-800 text-gray-400">
                        {appt.channel}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <StatusBadge status={appt.status} />
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        {appt.status === "CONFIRMED" && (
                          <button
                            onClick={() => updateStatus({ id: appt.id, status: "COMPLETED" })}
                            disabled={updating}
                            className="p-1.5 rounded-lg hover:bg-emerald-500/10 text-gray-500 hover:text-emerald-400 transition-colors"
                            title="Marcar completada"
                          >
                            <CheckCircle className="w-4 h-4" />
                          </button>
                        )}
                        {appt.status !== "CANCELLED" && (
                          <button
                            onClick={() => cancelAppointment({ id: appt.id })}
                            disabled={cancelling}
                            className="p-1.5 rounded-lg hover:bg-red-500/10 text-gray-500 hover:text-red-400 transition-colors"
                            title="Cancelar"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-gray-800/50">
            <p className="text-xs text-gray-500">
              {data.totalElements} reservas totales
            </p>
            <div className="flex gap-2">
              <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                ← Anterior
              </Button>
              <span className="text-xs text-gray-400 flex items-center px-2">
                {page + 1} / {data.totalPages}
              </span>
              <Button variant="ghost" size="sm" disabled={data.last} onClick={() => setPage(p => p + 1)}>
                Siguiente →
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
