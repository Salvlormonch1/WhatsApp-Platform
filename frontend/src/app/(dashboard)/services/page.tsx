"use client";

import { useState } from "react";
import { useServices, useCreateService, useUpdateService, useDeleteService } from "@/lib/api/hooks";
import { PageLoader, EmptyState, PageHeader, Button } from "@/components/ui/Shared";
import { formatCurrency, formatDuration } from "@/lib/utils";
import { Package, Plus, Edit2, Trash2, X, Check } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { Service } from "@/types";

interface ServiceFormData {
  name: string;
  description: string;
  price: string;
  durationMinutes: string;
}

const EMPTY_FORM: ServiceFormData = { name: "", description: "", price: "", durationMinutes: "30" };

export default function ServicesPage() {
  const { data: services, isLoading } = useServices();
  const { mutate: createService, isPending: creating } = useCreateService();
  const { mutate: updateService, isPending: updating } = useUpdateService();
  const { mutate: deleteService, isPending: deleting } = useDeleteService();

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<ServiceFormData>(EMPTY_FORM);

  const set = (k: keyof ServiceFormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSubmit = () => {
    const data = {
      name: form.name,
      description: form.description,
      price: parseFloat(form.price),
      durationMinutes: parseInt(form.durationMinutes),
    };
    if (editingId) {
      updateService({ id: editingId, data }, {
        onSuccess: () => { setEditingId(null); setForm(EMPTY_FORM); }
      });
    } else {
      createService(data, {
        onSuccess: () => { setShowForm(false); setForm(EMPTY_FORM); }
      });
    }
  };

  const startEdit = (s: Service) => {
    setEditingId(s.id);
    setForm({ name: s.name, description: s.description ?? "", price: String(s.price), durationMinutes: String(s.durationMinutes) });
  };

  const inputCls = "w-full px-3 py-2 rounded-lg bg-gray-800/60 border border-gray-700/50 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500 text-sm";

  return (
    <div className="fade-in">
      <PageHeader
        title="Servicios"
        subtitle="Gestiona tu catálogo de servicios"
        action={
          <Button variant="primary" onClick={() => { setShowForm(true); setEditingId(null); setForm(EMPTY_FORM); }}>
            <Plus className="w-4 h-4" />
            Nuevo servicio
          </Button>
        }
      />

      {/* Create form */}
      {showForm && (
        <div className="mb-6 rounded-2xl bg-gray-900/50 border border-indigo-500/30 p-5">
          <h3 className="text-sm font-semibold text-gray-200 mb-4">Nuevo servicio</h3>
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2">
              <label className="text-xs text-gray-400 mb-1 block">Nombre del servicio *</label>
              <input type="text" value={form.name} onChange={set("name")} placeholder="Ej: Corte de cabello" className={inputCls} />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Precio (S/.)*</label>
              <input type="number" value={form.price} onChange={set("price")} placeholder="25.00" step="0.50" className={inputCls} />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Duración (minutos)*</label>
              <input type="number" value={form.durationMinutes} onChange={set("durationMinutes")} placeholder="30" className={inputCls} />
            </div>
            <div className="col-span-2">
              <label className="text-xs text-gray-400 mb-1 block">Descripción</label>
              <input type="text" value={form.description} onChange={set("description")} placeholder="Descripción opcional" className={inputCls} />
            </div>
          </div>
          <div className="flex gap-2 mt-4">
            <Button variant="primary" onClick={handleSubmit} disabled={creating || !form.name || !form.price}>
              {creating ? "Guardando…" : "Guardar servicio"}
            </Button>
            <Button variant="ghost" onClick={() => setShowForm(false)}>Cancelar</Button>
          </div>
        </div>
      )}

      {isLoading ? (
        <PageLoader />
      ) : !services?.length ? (
        <EmptyState
          icon={<Package className="w-7 h-7" />}
          title="Sin servicios"
          description="Agrega los servicios que ofreces para que la IA pueda gestionarlos"
          action={
            <Button variant="primary" onClick={() => setShowForm(true)}>
              <Plus className="w-4 h-4" />
              Agregar primer servicio
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {services.map((s) => (
            <div key={s.id} className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5 hover:border-gray-700/60 transition-colors group">
              {editingId === s.id ? (
                <div className="space-y-2">
                  <input type="text" value={form.name} onChange={set("name")} className={inputCls} />
                  <div className="grid grid-cols-2 gap-2">
                    <input type="number" value={form.price} onChange={set("price")} className={inputCls} placeholder="Precio" />
                    <input type="number" value={form.durationMinutes} onChange={set("durationMinutes")} className={inputCls} placeholder="Min" />
                  </div>
                  <input type="text" value={form.description} onChange={set("description")} className={inputCls} placeholder="Descripción" />
                  <div className="flex gap-2 mt-2">
                    <button onClick={handleSubmit} disabled={updating} className="p-1.5 rounded-lg bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 transition-colors">
                      <Check className="w-4 h-4" />
                    </button>
                    <button onClick={() => setEditingId(null)} className="p-1.5 rounded-lg bg-gray-800 text-gray-400 hover:text-gray-200 transition-colors">
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <div className="flex items-start justify-between mb-3">
                    <div className="w-9 h-9 rounded-xl bg-indigo-500/10 flex items-center justify-center">
                      <Package className="w-4 h-4 text-indigo-400" />
                    </div>
                    <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={() => startEdit(s)} className="p-1.5 rounded-lg hover:bg-gray-800 text-gray-500 hover:text-gray-200 transition-colors">
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button onClick={() => deleteService(s.id)} disabled={deleting} className="p-1.5 rounded-lg hover:bg-red-500/10 text-gray-500 hover:text-red-400 transition-colors">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                  <h3 className="font-semibold text-gray-100 text-sm mb-1">{s.name}</h3>
                  {s.description && <p className="text-xs text-gray-500 mb-3 line-clamp-2">{s.description}</p>}
                  <div className="flex items-center justify-between">
                    <span className="text-lg font-bold text-indigo-400">{formatCurrency(s.price)}</span>
                    <span className="text-xs px-2 py-1 rounded-lg bg-gray-800 text-gray-400">{formatDuration(s.durationMinutes)}</span>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
