import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { format, formatDistanceToNow, parseISO } from "date-fns";
import { es } from "date-fns/locale";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(date: string | Date, fmt = "dd/MM/yyyy") {
  const d = typeof date === "string" ? parseISO(date) : date;
  return format(d, fmt, { locale: es });
}

export function formatDateTime(date: string | Date) {
  const d = typeof date === "string" ? parseISO(date) : date;
  return format(d, "dd/MM/yyyy HH:mm", { locale: es });
}

export function formatRelative(date: string | Date) {
  const d = typeof date === "string" ? parseISO(date) : date;
  return formatDistanceToNow(d, { addSuffix: true, locale: es });
}

export function formatCurrency(amount: number) {
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN",
  }).format(amount);
}

export function formatDuration(minutes: number) {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}min` : `${h}h`;
}

export function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    // Appointments
    PENDING: "bg-amber-500/15 text-amber-600 border-amber-500/20",
    CONFIRMED: "bg-emerald-500/15 text-emerald-600 border-emerald-500/20",
    CANCELLED: "bg-red-500/15 text-red-600 border-red-500/20",
    COMPLETED: "bg-blue-500/15 text-blue-600 border-blue-500/20",
    NO_SHOW: "bg-gray-500/15 text-gray-600 border-gray-500/20",
    // Conversations
    AI_ACTIVE: "bg-emerald-500/15 text-emerald-600 border-emerald-500/20",
    WAITING_HUMAN: "bg-red-500/15 text-red-600 border-red-500/20",
    HUMAN_ACTIVE: "bg-blue-500/15 text-blue-600 border-blue-500/20",
    RESOLVED: "bg-gray-500/15 text-gray-600 border-gray-500/20",
  };
  return map[status] ?? "bg-gray-500/15 text-gray-600";
}

export function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: "Pendiente",
    CONFIRMED: "Confirmada",
    CANCELLED: "Cancelada",
    COMPLETED: "Completada",
    NO_SHOW: "No asistió",
    AI_ACTIVE: "IA activa",
    WAITING_HUMAN: "Espera humano",
    HUMAN_ACTIVE: "Humano activo",
    RESOLVED: "Resuelta",
    BARBERSHOP: "Barbería",
    HAIR_SALON: "Peluquería",
    BEAUTY_SALON: "Salón de belleza",
    NAIL_SALON: "Salón de uñas",
    SPA: "Spa",
    OTHER: "Otro",
  };
  return map[status] ?? status;
}
