"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/auth/AuthContext";
import { ApiError } from "@/lib/api/client";
import { Loader2, Zap } from "lucide-react";

export default function RegisterPage() {
  const { register } = useAuth();
  const [form, setForm] = useState({
    firstName: "", lastName: "", email: "", password: "",
    businessName: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await register(form);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Error al registrarse");
    } finally {
      setLoading(false);
    }
  };

  const inputCls =
    "w-full px-4 py-3 rounded-xl bg-gray-900/60 border border-gray-700/50 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50 transition-all text-sm";

  return (
    <div className="fade-in">
      <div className="flex items-center justify-center gap-3 mb-8">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/25">
          <Zap className="w-5 h-5 text-white" />
        </div>
        <span className="text-xl font-bold text-white">SaaS Platform</span>
      </div>

      <div className="glass rounded-2xl p-8 shadow-2xl shadow-black/40">
        <h1 className="text-2xl font-bold text-white mb-1">Crea tu cuenta</h1>
        <p className="text-sm text-gray-400 mb-8">Configura tu negocio en minutos</p>

        {error && (
          <div className="mb-5 px-4 py-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1.5">Nombre</label>
              <input id="firstName" type="text" required value={form.firstName} onChange={set("firstName")} placeholder="Juan" className={inputCls} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1.5">Apellido</label>
              <input id="lastName" type="text" required value={form.lastName} onChange={set("lastName")} placeholder="Pérez" className={inputCls} />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-400 mb-1.5">Correo electrónico</label>
            <input id="reg-email" type="email" required value={form.email} onChange={set("email")} placeholder="tu@negocio.com" className={inputCls} />
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-400 mb-1.5">Contraseña</label>
            <input id="reg-password" type="password" required minLength={8} value={form.password} onChange={set("password")} placeholder="Mínimo 8 caracteres" className={inputCls} />
          </div>

          <div className="border-t border-gray-700/50 pt-4 mt-2">
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Tu negocio</p>
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1.5">Nombre del negocio</label>
              <input id="businessName" type="text" required value={form.businessName} onChange={set("businessName")} placeholder="Mi Negocio" className={inputCls} />
            </div>
          </div>

          <button
            id="register-submit"
            type="submit"
            disabled={loading}
            className="w-full py-3 mt-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-semibold text-sm transition-all duration-200 disabled:opacity-60 flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/20"
          >
            {loading ? <><Loader2 className="w-4 h-4 animate-spin" />Creando cuenta...</> : "Crear cuenta gratis"}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-gray-500">
          ¿Ya tienes cuenta?{" "}
          <Link href="/login" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
            Inicia sesión
          </Link>
        </p>
      </div>
    </div>
  );
}
