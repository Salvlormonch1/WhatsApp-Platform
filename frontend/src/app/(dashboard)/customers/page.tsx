"use client";

import { useState } from "react";
import { useCustomers } from "@/lib/api/hooks";
import { PageLoader, EmptyState, PageHeader, Button } from "@/components/ui/Shared";
import { formatRelative, formatDate } from "@/lib/utils";
import { Users, Search, Phone, Mail, MessageSquare } from "lucide-react";

export default function CustomersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [inputVal, setInputVal] = useState("");

  const { data, isLoading } = useCustomers(page, 20, search || undefined);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(inputVal);
    setPage(0);
  };

  return (
    <div className="fade-in">
      <PageHeader
        title="Clientes"
        subtitle="Clientes que contactaron via WhatsApp"
      />

      {/* Search bar */}
      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
          <input
            type="text"
            value={inputVal}
            onChange={(e) => setInputVal(e.target.value)}
            placeholder="Buscar por nombre o teléfono..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-gray-900/60 border border-gray-700/50 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500 text-sm"
          />
        </div>
        <Button variant="secondary" type="submit">Buscar</Button>
        {search && (
          <Button variant="ghost" onClick={() => { setSearch(""); setInputVal(""); }}>
            Limpiar
          </Button>
        )}
      </form>

      {/* Stats strip */}
      {data && (
        <div className="flex items-center gap-2 mb-5">
          <span className="text-xs text-gray-500">{data.totalElements} clientes en total</span>
          {search && (
            <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              Filtrado: "{search}"
            </span>
          )}
        </div>
      )}

      {isLoading ? (
        <PageLoader />
      ) : !data?.content?.length ? (
        <EmptyState
          icon={<Users className="w-7 h-7" />}
          title={search ? "Sin resultados" : "Sin clientes aún"}
          description={search ? `No se encontraron clientes con "${search}"` : "Los clientes se crean automáticamente cuando escriben por WhatsApp"}
        />
      ) : (
        <>
          <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-800/60">
                    <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Cliente</th>
                    <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Teléfono</th>
                    <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Primer contacto</th>
                    <th className="text-left px-5 py-3.5 text-xs font-medium text-gray-500 uppercase tracking-wider">Último contacto</th>
                    <th className="px-5 py-3.5" />
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((customer) => (
                    <tr key={customer.id} className="border-b border-gray-800/30 hover:bg-gray-800/20 transition-colors group">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500/20 to-violet-500/20 border border-gray-700/50 flex items-center justify-center text-xs font-bold text-gray-300 flex-shrink-0">
                            {(customer.name ?? customer.phone).charAt(0).toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <p className="font-medium text-gray-200 truncate">
                              {customer.name ?? <span className="text-gray-500 italic">Sin nombre</span>}
                            </p>
                            {customer.email && (
                              <p className="text-xs text-gray-500 flex items-center gap-1">
                                <Mail className="w-3 h-3" />{customer.email}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="flex items-center gap-1.5 text-gray-300">
                          <Phone className="w-3.5 h-3.5 text-emerald-400" />
                          {customer.phone}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-gray-400 text-xs">
                        {formatDate(customer.firstContactAt)}
                      </td>
                      <td className="px-5 py-3.5 text-gray-400 text-xs">
                        {formatRelative(customer.lastContactAt)}
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                          <a
                            href={`https://wa.me/${customer.phone.replace("+", "")}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="p-1.5 rounded-lg hover:bg-emerald-500/10 text-gray-500 hover:text-emerald-400 transition-colors"
                            title="Abrir en WhatsApp"
                          >
                            <MessageSquare className="w-4 h-4" />
                          </a>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {data.totalPages > 1 && (
              <div className="flex items-center justify-between px-5 py-3 border-t border-gray-800/50">
                <p className="text-xs text-gray-500">
                  Página {page + 1} de {data.totalPages} · {data.totalElements} clientes
                </p>
                <div className="flex gap-2">
                  <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    ← Anterior
                  </Button>
                  <Button variant="ghost" size="sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
                    Siguiente →
                  </Button>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
