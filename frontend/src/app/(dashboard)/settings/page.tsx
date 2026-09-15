"use client";

import { useState } from "react";
import { useBusiness, useBusinessConfig, useBusinessHours } from "@/lib/api/hooks";
import { businessApi, whatsappApi } from "@/lib/api/endpoints";
import { PageLoader, PageHeader, Button } from "@/components/ui/Shared";
import { useQueryClient, useMutation, useQuery } from "@tanstack/react-query";
import {
  Building2, Bot, Clock, Bell, Smartphone,
  Check, Save, Wifi, WifiOff, Copy, ExternalLink, CheckCircle2, AlertCircle, Send,
} from "lucide-react";
import { cn } from "@/lib/utils";

const TABS = [
  { id: "business",      label: "Negocio",         icon: Building2 },
  { id: "ai",            label: "Asistente IA",    icon: Bot },
  { id: "hours",         label: "Horarios",        icon: Clock },
  { id: "whatsapp",      label: "WhatsApp",        icon: Smartphone },
  { id: "notifications", label: "Notificaciones",  icon: Bell },
] as const;

type TabId = (typeof TABS)[number]["id"];

const DAY_NAMES = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];
const TIMEZONES = ["America/Lima", "America/Bogota", "America/Mexico_City", "America/Santiago", "America/Buenos_Aires", "UTC"];
const AI_TONES = [{ v: "FORMAL", l: "Formal" }, { v: "FRIENDLY", l: "Amigable" }, { v: "CASUAL", l: "Casual" }];

const inputCls = "w-full px-3 py-2.5 rounded-xl bg-gray-900/60 border border-gray-700/50 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500 text-sm transition-all";
const labelCls = "block text-xs font-medium text-gray-400 mb-1.5";

export default function SettingsPage() {
  const [tab, setTab] = useState<TabId>("business");
  const qc = useQueryClient();

  const { data: business, isLoading: bizLoading } = useBusiness();
  const { data: config, isLoading: cfgLoading } = useBusinessConfig();
  const { data: hours, isLoading: hrsLoading } = useBusinessHours();

  // ---- Business profile mutation ----
  const [bizForm, setBizForm] = useState<any>(null);
  const { mutate: saveBusiness, isPending: savingBiz } = useMutation({
    mutationFn: (data: any) => businessApi.update(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["business"] }),
  });

  // ---- Config mutation ----
  const [cfgForm, setCfgForm] = useState<any>(null);
  const { mutate: saveConfig, isPending: savingCfg } = useMutation({
    mutationFn: (data: any) => businessApi.updateConfig(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["business-config"] }),
  });

  // ---- Hours mutation ----
  const [hoursForm, setHoursForm] = useState<any[] | null>(null);
  const { mutate: saveHours, isPending: savingHrs } = useMutation({
    mutationFn: (data: any[]) => businessApi.updateHours(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["business-hours"] }),
  });

  if (bizLoading || cfgLoading || hrsLoading) return <PageLoader />;

  // Initialize forms on first render
  const biz = bizForm ?? business ?? {};
  const cfg = cfgForm ?? config ?? {};
  const hrsData: any[] = hoursForm ?? hours ?? Array.from({ length: 7 }, (_, i) => ({
    dayOfWeek: i, openTime: "09:00", closeTime: "18:00", isClosed: i === 0,
  }));

  return (
    <div className="fade-in">
      <PageHeader title="Configuración" subtitle="Personaliza tu negocio y el asistente de IA" />

      {/* Tab navigation */}
      <div className="flex gap-1 p-1 rounded-xl bg-gray-900/60 border border-gray-800/50 w-fit mb-7">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              tab === id ? "bg-indigo-600 text-white" : "text-gray-400 hover:text-gray-200"
            }`}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* ---- Business Tab ---- */}
      {tab === "business" && (
        <div className="max-w-2xl space-y-5">
          <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-6 space-y-4">
            <h3 className="text-sm font-semibold text-gray-200 mb-2">Información del negocio</h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <label className={labelCls}>Nombre del negocio</label>
                <input type="text" className={inputCls} value={biz.name ?? ""} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, name: e.target.value }))} />
              </div>
              <div className="col-span-2">
                <label className={labelCls}>Descripción</label>
                <textarea rows={2} className={inputCls + " resize-none"} value={biz.description ?? ""} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, description: e.target.value }))} />
              </div>
              <div>
                <label className={labelCls}>Teléfono</label>
                <input type="tel" className={inputCls} value={biz.phone ?? ""} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, phone: e.target.value }))} />
              </div>
              <div>
                <label className={labelCls}>Email</label>
                <input type="email" className={inputCls} value={biz.email ?? ""} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, email: e.target.value }))} />
              </div>
              <div className="col-span-2">
                <label className={labelCls}>Dirección</label>
                <input type="text" className={inputCls} value={biz.address ?? ""} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, address: e.target.value }))} />
              </div>
              <div>
                <label className={labelCls}>Zona horaria</label>
                <select className={inputCls + " cursor-pointer"} value={biz.timezone ?? "UTC"} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, timezone: e.target.value }))}>
                  {TIMEZONES.map((tz) => <option key={tz} value={tz}>{tz}</option>)}
                </select>
              </div>
              <div>
                <label className={labelCls}>Sitio web</label>
                <input type="url" className={inputCls} value={biz.website ?? ""} onChange={(e) => setBizForm((f: any) => ({ ...biz, ...f, website: e.target.value }))} />
              </div>
            </div>
          </div>
          <Button variant="primary" onClick={() => saveBusiness(biz)} disabled={savingBiz}>
            <Save className="w-4 h-4" />
            {savingBiz ? "Guardando…" : "Guardar cambios"}
          </Button>
        </div>
      )}

      {/* ---- AI Tab ---- */}
      {tab === "ai" && (
        <div className="max-w-2xl space-y-5">
          <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-6 space-y-4">
            <h3 className="text-sm font-semibold text-gray-200 mb-1">Asistente de IA</h3>
            <p className="text-xs text-gray-500 mb-4">
              Configura cómo se comporta el asistente con tus clientes
            </p>
            <div>
              <label className={labelCls}>Nombre del asistente</label>
              <input type="text" className={inputCls} placeholder="Ej: Luna, Asistente, Alex" value={cfg.aiAssistantName ?? ""} onChange={(e) => setCfgForm((f: any) => ({ ...cfg, ...f, aiAssistantName: e.target.value }))} />
            </div>
            <div>
              <label className={labelCls}>Tono de comunicación</label>
              <div className="flex gap-2">
                {AI_TONES.map(({ v, l }) => (
                  <button
                    key={v}
                    onClick={() => setCfgForm((f: any) => ({ ...cfg, ...f, aiTone: v }))}
                    className={`flex-1 py-2 rounded-xl text-sm font-medium border transition-all ${
                      (cfg.aiTone ?? "FRIENDLY") === v
                        ? "bg-indigo-600/20 border-indigo-500/50 text-indigo-300"
                        : "bg-gray-900/60 border-gray-700/50 text-gray-400 hover:border-gray-600"
                    }`}
                  >
                    {l}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className={labelCls}>Reglas e instrucciones adicionales</label>
              <textarea
                rows={5}
                className={inputCls + " resize-none"}
                placeholder={"Ej:\n- No aceptar reservas de más de 2 personas\n- Dar descuento del 10% a clientes frecuentes\n- Recomendar siempre el servicio premium"}
                value={cfg.aiCustomRules ?? ""}
                onChange={(e) => setCfgForm((f: any) => ({ ...cfg, ...f, aiCustomRules: e.target.value }))}
              />
              <p className="text-xs text-gray-500 mt-1.5">Estas reglas se incluyen en el prompt del asistente</p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>Antelación mínima (minutos)</label>
                <input type="number" className={inputCls} value={cfg.bookingLeadTimeMinutes ?? 60} onChange={(e) => setCfgForm((f: any) => ({ ...cfg, ...f, bookingLeadTimeMinutes: parseInt(e.target.value) }))} />
              </div>
              <div>
                <label className={labelCls}>Máx. días a futuro</label>
                <input type="number" className={inputCls} value={cfg.bookingMaxDaysAhead ?? 30} onChange={(e) => setCfgForm((f: any) => ({ ...cfg, ...f, bookingMaxDaysAhead: parseInt(e.target.value) }))} />
              </div>
            </div>
          </div>
          <Button variant="primary" onClick={() => saveConfig(cfg)} disabled={savingCfg}>
            <Save className="w-4 h-4" />
            {savingCfg ? "Guardando…" : "Guardar configuración IA"}
          </Button>
        </div>
      )}

      {/* ---- Hours Tab ---- */}
      {tab === "hours" && (
        <div className="max-w-2xl space-y-5">
          <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-6">
            <h3 className="text-sm font-semibold text-gray-200 mb-5">Horarios de atención</h3>
            <div className="space-y-3">
              {hrsData.map((h: any, i: number) => (
                <div key={i} className="flex items-center gap-4 py-2 border-b border-gray-800/40 last:border-0">
                  <span className="text-sm font-medium text-gray-300 w-24">{DAY_NAMES[h.dayOfWeek]}</span>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <div
                      onClick={() => setHoursForm(hrsData.map((x, j) => j === i ? { ...x, isClosed: !x.isClosed } : x))}
                      className={`w-10 h-5 rounded-full transition-colors cursor-pointer ${h.isClosed ? "bg-gray-700" : "bg-indigo-600"}`}
                    >
                      <div className={`w-4 h-4 rounded-full bg-white m-0.5 transition-transform ${h.isClosed ? "" : "translate-x-5"}`} />
                    </div>
                    <span className="text-xs text-gray-400">{h.isClosed ? "Cerrado" : "Abierto"}</span>
                  </label>
                  {!h.isClosed && (
                    <>
                      <input
                        type="time"
                        value={h.openTime ?? "09:00"}
                        onChange={(e) => setHoursForm(hrsData.map((x, j) => j === i ? { ...x, openTime: e.target.value } : x))}
                        className="px-2 py-1 rounded-lg bg-gray-800 border border-gray-700/50 text-sm text-gray-200 focus:outline-none focus:border-indigo-500"
                      />
                      <span className="text-gray-500 text-sm">—</span>
                      <input
                        type="time"
                        value={h.closeTime ?? "18:00"}
                        onChange={(e) => setHoursForm(hrsData.map((x, j) => j === i ? { ...x, closeTime: e.target.value } : x))}
                        className="px-2 py-1 rounded-lg bg-gray-800 border border-gray-700/50 text-sm text-gray-200 focus:outline-none focus:border-indigo-500"
                      />
                    </>
                  )}
                </div>
              ))}
            </div>
          </div>
          <Button variant="primary" onClick={() => saveHours(hrsData)} disabled={savingHrs}>
            <Save className="w-4 h-4" />
            {savingHrs ? "Guardando…" : "Guardar horarios"}
          </Button>
        </div>
      )}

      {/* ---- Notifications Tab ---- */}
      {/* ---- WhatsApp Tab ---- */}
      {tab === "whatsapp" && <WhatsAppTab />}

      {tab === "notifications" && (
        <div className="max-w-2xl space-y-5">
          <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-6 space-y-4">
            <h3 className="text-sm font-semibold text-gray-200">Notificaciones por email</h3>

            {/* Toggle weekly report */}
            <div className="flex items-center justify-between py-3 border-b border-gray-800/40">
              <div>
                <p className="text-sm font-medium text-gray-200">Reporte semanal automático</p>
                <p className="text-xs text-gray-500">Recibirás un email cada lunes con el resumen de la semana</p>
              </div>
              <div
                onClick={() => setCfgForm((f: any) => ({ ...cfg, ...f, weeklyReportEnabled: !cfg.weeklyReportEnabled }))}
                className={`w-11 h-6 rounded-full transition-colors cursor-pointer ${cfg.weeklyReportEnabled ? "bg-indigo-600" : "bg-gray-700"}`}
              >
                <div className={`w-5 h-5 rounded-full bg-white m-0.5 transition-transform ${cfg.weeklyReportEnabled ? "translate-x-5" : ""}`} />
              </div>
            </div>

            <div>
              <label className={labelCls}>Emails para notificaciones</label>
              <input
                type="text"
                className={inputCls}
                placeholder="email1@negocio.com, email2@negocio.com"
                value={(cfg.notificationEmails ?? []).join(", ")}
                onChange={(e) => setCfgForm((f: any) => ({
                  ...cfg, ...f,
                  notificationEmails: e.target.value.split(",").map((s: string) => s.trim()).filter(Boolean)
                }))}
              />
              <p className="text-xs text-gray-500 mt-1.5">Separa múltiples emails con coma</p>
            </div>

            {/* WhatsApp setup info */}
            <div className="mt-4 p-4 rounded-xl bg-emerald-500/5 border border-emerald-500/20">
              <div className="flex items-start gap-3">
                <Smartphone className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-sm font-semibold text-gray-200">Integración WhatsApp Business</p>
                  <p className="text-xs text-gray-400 mt-1">
                    Configura tu WhatsApp Business API desde el panel de Meta for Developers.
                    Usa el webhook URL:{" "}
                    <code className="px-1 py-0.5 rounded bg-gray-800 text-indigo-300 text-xs">
                      {process.env.NEXT_PUBLIC_API_URL?.replace("/api/v1", "") ?? "https://tu-backend.com"}/api/v1/whatsapp/webhook
                    </code>
                  </p>
                </div>
              </div>
            </div>
          </div>

          <Button variant="primary" onClick={() => saveConfig(cfg)} disabled={savingCfg}>
            <Save className="w-4 h-4" />
            {savingCfg ? "Guardando…" : "Guardar notificaciones"}
          </Button>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// WhatsApp Tab — standalone sub-component
// ─────────────────────────────────────────────
function WhatsAppTab() {
  const qc = useQueryClient();

  const { data: statusData, isLoading } = useQuery({
    queryKey: ["whatsapp-integration"],
    queryFn: whatsappApi.getStatus,
  });

  const status = statusData?.data;
  const connected = status?.connected ?? false;

  const [form, setForm] = useState({ phoneNumberId: "", accessToken: "", wabaid: "", displayPhone: "" });
  const [testPhone, setTestPhone] = useState("");
  const [copied, setCopied] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const { mutate: connect, isPending: connecting } = useMutation({
    mutationFn: () => whatsappApi.connect({
      phoneNumberId: form.phoneNumberId,
      accessToken: form.accessToken,
      whatsappBusinessAccountId: form.wabaid || undefined,
      displayPhone: form.displayPhone || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["whatsapp-integration"] });
      setShowForm(false);
      setForm({ phoneNumberId: "", accessToken: "", wabaid: "", displayPhone: "" });
    },
  });

  const { mutate: disconnect, isPending: disconnecting } = useMutation({
    mutationFn: whatsappApi.disconnect,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["whatsapp-integration"] }),
  });

  const { mutate: testConn, isPending: testing, data: testResult } = useMutation({
    mutationFn: () => whatsappApi.test(testPhone),
  });

  const webhookUrl = status?.webhookUrl ?? "https://tu-plataforma.com/api/v1/whatsapp/webhook";
  const verifyToken = process.env.NEXT_PUBLIC_WHATSAPP_VERIFY_TOKEN ?? "(ver .env → WHATSAPP_VERIFY_TOKEN)";

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const inputCls = "w-full px-3 py-2.5 rounded-xl bg-gray-900/60 border border-gray-700/50 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500 text-sm transition-all";
  const labelCls = "block text-xs font-medium text-gray-400 mb-1.5";

  if (isLoading) return <div className="h-40 flex items-center justify-center text-gray-500 text-sm">Cargando...</div>;

  return (
    <div className="max-w-2xl space-y-4">

      {/* ── Status banner ── */}
      <div className={`rounded-2xl border p-5 flex items-center gap-4 ${
        connected
          ? "bg-emerald-500/5 border-emerald-500/20"
          : "bg-gray-900/50 border-gray-800/60"
      }`}>
        <div className={`p-3 rounded-xl ${connected ? "bg-emerald-500/10" : "bg-gray-800"}`}>
          {connected
            ? <Wifi className="w-6 h-6 text-emerald-400" />
            : <WifiOff className="w-6 h-6 text-gray-500" />}
        </div>
        <div className="flex-1">
          <p className={`font-semibold ${connected ? "text-emerald-300" : "text-gray-300"}`}>
            {connected ? "WhatsApp conectado" : "WhatsApp no conectado"}
          </p>
          {connected ? (
            <p className="text-xs text-gray-400 mt-0.5">
              {status.displayPhone && <span className="mr-2">📱 {status.displayPhone}</span>}
              ID: <code className="text-indigo-300">{status.phoneNumberId}</code>
            </p>
          ) : (
            <p className="text-xs text-gray-500 mt-0.5">
              Conecta tu número de WhatsApp Business para que la IA atienda mensajes
            </p>
          )}
        </div>
        {connected ? (
          <Button variant="ghost" size="sm"
            onClick={() => disconnect()} disabled={disconnecting}
            className="text-red-400 hover:text-red-300 border-red-500/20 hover:border-red-500/40">
            <WifiOff className="w-4 h-4" />
            {disconnecting ? "Desconectando…" : "Desconectar"}
          </Button>
        ) : (
          <Button variant="primary" size="sm" onClick={() => setShowForm(!showForm)}>
            <Wifi className="w-4 h-4" />
            Conectar
          </Button>
        )}
      </div>

      {/* ── Webhook info (always visible) ── */}
      <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5 space-y-4">
        <h3 className="text-sm font-semibold text-gray-200">Datos para configurar en Meta</h3>
        <p className="text-xs text-gray-500">
          En el panel de Meta for Developers, en tu app de WhatsApp, configura estos valores exactos:
        </p>

        <div>
          <label className={labelCls}>URL del Webhook</label>
          <div className="flex gap-2">
            <code className="flex-1 px-3 py-2 rounded-xl bg-gray-950/60 border border-gray-700/40 text-indigo-300 text-xs break-all">
              {webhookUrl}
            </code>
            <button
              onClick={() => copyToClipboard(webhookUrl)}
              className="px-3 rounded-xl bg-gray-800 border border-gray-700/50 text-gray-400 hover:text-gray-200 transition-colors flex-shrink-0"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <div>
          <label className={labelCls}>Token de verificación del Webhook</label>
          <div className="flex gap-2">
            <code className="flex-1 px-3 py-2 rounded-xl bg-gray-950/60 border border-gray-700/40 text-amber-300 text-xs">
              {verifyToken}
            </code>
            <button
              onClick={() => copyToClipboard(verifyToken)}
              className="px-3 rounded-xl bg-gray-800 border border-gray-700/50 text-gray-400 hover:text-gray-200 transition-colors flex-shrink-0"
            >
              <Copy className="w-4 h-4" />
            </button>
          </div>
          <p className="text-xs text-gray-600 mt-1">Este valor está en tu archivo .env → WHATSAPP_VERIFY_TOKEN</p>
        </div>

        <div className="p-3 rounded-xl bg-blue-500/5 border border-blue-500/15">
          <p className="text-xs text-blue-300 font-medium mb-1">Suscripciones requeridas en el webhook:</p>
          <div className="flex gap-2 flex-wrap">
            {["messages", "message_deliveries", "message_reads"].map(s => (
              <span key={s} className="px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-300 text-xs border border-blue-500/20">
                ✓ {s}
              </span>
            ))}
          </div>
        </div>

        <a
          href="https://developers.facebook.com/apps"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 transition-colors"
        >
          <ExternalLink className="w-3.5 h-3.5" />
          Abrir Meta for Developers
        </a>
      </div>

      {/* ── Connect form ── */}
      {(showForm || !connected) && !connected && (
        <div className="rounded-2xl bg-gray-900/50 border border-indigo-500/20 p-5 space-y-4">
          <h3 className="text-sm font-semibold text-gray-200">Conectar tu número</h3>
          <p className="text-xs text-gray-500">
            Estos datos los obtienes en Meta for Developers → tu app → WhatsApp → API Setup
          </p>

          <div>
            <label className={labelCls}>Phone Number ID <span className="text-red-400">*</span></label>
            <input
              type="text"
              className={inputCls}
              placeholder="Ej: 123456789012345"
              value={form.phoneNumberId}
              onChange={e => setForm(f => ({ ...f, phoneNumberId: e.target.value }))}
            />
            <p className="text-xs text-gray-600 mt-1">Es el ID numérico de 15 dígitos, NO el número de teléfono</p>
          </div>

          <div>
            <label className={labelCls}>Access Token <span className="text-red-400">*</span></label>
            <input
              type="password"
              className={inputCls}
              placeholder="EAA..."
              value={form.accessToken}
              onChange={e => setForm(f => ({ ...f, accessToken: e.target.value }))}
            />
            <p className="text-xs text-gray-600 mt-1">Se guardará cifrado con AES-256 — nunca se muestra en texto plano</p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>WhatsApp Business Account ID</label>
              <input
                type="text"
                className={inputCls}
                placeholder="Opcional"
                value={form.wabaid}
                onChange={e => setForm(f => ({ ...f, wabaid: e.target.value }))}
              />
            </div>
            <div>
              <label className={labelCls}>Número de teléfono visible</label>
              <input
                type="text"
                className={inputCls}
                placeholder="+51 999 999 999"
                value={form.displayPhone}
                onChange={e => setForm(f => ({ ...f, displayPhone: e.target.value }))}
              />
            </div>
          </div>

          <div className="flex gap-2 pt-1">
            <Button
              variant="primary"
              onClick={() => connect()}
              disabled={connecting || !form.phoneNumberId || !form.accessToken}
            >
              <Wifi className="w-4 h-4" />
              {connecting ? "Conectando…" : "Conectar WhatsApp"}
            </Button>
            {connected && (
              <Button variant="ghost" onClick={() => setShowForm(false)}>Cancelar</Button>
            )}
          </div>
        </div>
      )}

      {/* ── Test connection (only when connected) ── */}
      {connected && (
        <div className="rounded-2xl bg-gray-900/50 border border-gray-800/60 p-5">
          <h3 className="text-sm font-semibold text-gray-200 mb-3">Probar la conexión</h3>
          <p className="text-xs text-gray-500 mb-3">
            Envía un mensaje de prueba a tu propio número para verificar que el bot puede responder
          </p>
          <div className="flex gap-2">
            <input
              type="tel"
              value={testPhone}
              onChange={e => setTestPhone(e.target.value)}
              placeholder="+51999999999 (con código de país)"
              className={inputCls}
            />
            <Button variant="secondary" onClick={() => testConn()} disabled={testing || !testPhone}>
              <Send className="w-4 h-4" />
              {testing ? "Probando…" : "Probar"}
            </Button>
          </div>
          {testResult && (
            <div className={`mt-3 p-3 rounded-xl text-xs flex items-center gap-2 ${
              testResult.success
                ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/20"
                : "bg-red-500/10 text-red-300 border border-red-500/20"
            }`}>
              {testResult.success
                ? <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                : <AlertCircle className="w-4 h-4 flex-shrink-0" />}
              {testResult.message}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
