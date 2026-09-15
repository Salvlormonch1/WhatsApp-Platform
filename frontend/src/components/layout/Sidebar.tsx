"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth/AuthContext";
import { cn, getStatusLabel } from "@/lib/utils";
import {
  LayoutDashboard, CalendarDays, Users, Package,
  MessageSquare, BarChart3, Settings, Zap, LogOut,
  ChevronRight, Sparkles,
} from "lucide-react";

const NAV_ITEMS = [
  { href: "/dashboard", label: "Resumen", icon: LayoutDashboard },
  { href: "/appointments", label: "Reservas", icon: CalendarDays },
  { href: "/conversations", label: "Conversaciones", icon: MessageSquare },
  { href: "/customers", label: "Clientes", icon: Users },
  { href: "/services", label: "Servicios", icon: Package },
  { href: "/reports", label: "Reportes", icon: BarChart3 },
  { href: "/settings", label: "Configuración", icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, business, logout } = useAuth();

  return (
    <aside className="fixed left-0 top-0 h-full w-64 bg-gray-900/80 backdrop-blur-xl border-r border-gray-800/50 flex flex-col z-40">
      {/* Logo */}
      <div className="px-5 py-6 border-b border-gray-800/50">
        <Link href="/dashboard" className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/25 flex-shrink-0">
            <Zap className="w-4 h-4 text-white" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold text-white truncate">
              {business?.name ?? "SaaS Platform"}
            </p>
            <p className="text-xs text-gray-500 flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-indigo-400" />
              Plan IA activo
            </p>
          </div>
        </Link>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
          const isActive = href === "/dashboard"
            ? pathname === "/dashboard"
            : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "nav-link flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium group",
                isActive
                  ? "bg-indigo-500/15 text-indigo-400"
                  : "text-gray-400 hover:text-gray-200"
              )}
            >
              <Icon
                className={cn(
                  "w-4 h-4 flex-shrink-0 transition-colors",
                  isActive ? "text-indigo-400" : "text-gray-500 group-hover:text-gray-300"
                )}
              />
              <span className="flex-1">{label}</span>
              {isActive && <ChevronRight className="w-3 h-3 text-indigo-400" />}
            </Link>
          );
        })}
      </nav>

      {/* User footer */}
      <div className="px-3 py-4 border-t border-gray-800/50">
        <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-gray-800/50 transition-colors group">
          <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
            {user?.firstName?.[0]?.toUpperCase() ?? "U"}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium text-gray-200 truncate">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-xs text-gray-500 truncate">{user?.role}</p>
          </div>
          <button
            onClick={logout}
            title="Cerrar sesión"
            className="opacity-0 group-hover:opacity-100 p-1 rounded-lg hover:bg-red-500/10 text-gray-500 hover:text-red-400 transition-all"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}
