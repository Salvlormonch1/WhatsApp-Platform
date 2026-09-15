"use client";

import { useState } from "react";
import { useConversations, useTakeOver, useResolveConversation } from "@/lib/api/hooks";
import { PageLoader, EmptyState, PageHeader } from "@/components/ui/Shared";
import { StatusBadge, ConversationStatusDot } from "@/components/ui/Badge";
import { formatRelative } from "@/lib/utils";
import { MessageSquare, Bot, User, CheckCheck } from "lucide-react";
import Link from "next/link";
import type { ConversationStatus } from "@/types";

const FILTERS: { label: string; value: ConversationStatus | "ALL" }[] = [
  { label: "Todas", value: "ALL" },
  { label: "IA activa", value: "AI_ACTIVE" },
  { label: "Espera humano", value: "WAITING_HUMAN" },
  { label: "Humano activo", value: "HUMAN_ACTIVE" },
  { label: "Resueltas", value: "RESOLVED" },
];

export default function ConversationsPage() {
  const [filter, setFilter] = useState<ConversationStatus | "ALL">("ALL");
  const { data: conversations, isLoading } = useConversations();
  const { mutate: takeOver } = useTakeOver();
  const { mutate: resolve } = useResolveConversation();

  const filtered = conversations?.filter(
    (c) => filter === "ALL" || c.status === filter
  ) ?? [];

  const counts = {
    ALL: conversations?.length ?? 0,
    AI_ACTIVE: conversations?.filter((c) => c.status === "AI_ACTIVE").length ?? 0,
    WAITING_HUMAN: conversations?.filter((c) => c.status === "WAITING_HUMAN").length ?? 0,
    HUMAN_ACTIVE: conversations?.filter((c) => c.status === "HUMAN_ACTIVE").length ?? 0,
    RESOLVED: conversations?.filter((c) => c.status === "RESOLVED").length ?? 0,
  };

  return (
    <div className="fade-in">
      <PageHeader
        title="Conversaciones"
        subtitle="Monitorea y gestiona las conversaciones de WhatsApp"
      />

      {/* Filters */}
      <div className="flex gap-1 p-1 rounded-xl bg-gray-900/60 border border-gray-800/50 w-fit mb-6 flex-wrap">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              filter === f.value ? "bg-indigo-600 text-white" : "text-gray-400 hover:text-gray-200"
            }`}
          >
            {f.label}
            <span className={`px-1.5 py-0.5 rounded-full text-xs ${filter === f.value ? "bg-white/20" : "bg-gray-800"}`}>
              {counts[f.value]}
            </span>
          </button>
        ))}
      </div>

      {/* Alert banner for escalated */}
      {counts.WAITING_HUMAN > 0 && (
        <div className="mb-5 px-4 py-3 rounded-xl bg-red-500/10 border border-red-500/20 flex items-center gap-3">
          <div className="w-2 h-2 rounded-full bg-red-400 pulse-ring flex-shrink-0" />
          <p className="text-sm text-red-300">
            <strong>{counts.WAITING_HUMAN}</strong> conversación{counts.WAITING_HUMAN > 1 ? "es" : ""} esperando atención humana
          </p>
        </div>
      )}

      {isLoading ? (
        <PageLoader />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={<MessageSquare className="w-7 h-7" />}
          title="Sin conversaciones"
          description="Las conversaciones de WhatsApp aparecerán aquí en tiempo real"
        />
      ) : (
        <div className="space-y-2">
          {filtered.map((conv) => (
            <Link
              key={conv.id}
              href={`/conversations/${conv.id}`}
              className="flex items-center gap-4 p-4 rounded-2xl bg-gray-900/50 border border-gray-800/60 hover:border-gray-700/60 hover:bg-gray-900/80 transition-all group"
            >
              {/* Avatar */}
              <div className="relative flex-shrink-0">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500/20 to-violet-500/20 border border-gray-700/50 flex items-center justify-center text-sm font-bold text-gray-300">
                  {conv.customerId.slice(0, 1).toUpperCase()}
                </div>
                <span className="absolute -bottom-0.5 -right-0.5">
                  <ConversationStatusDot status={conv.status} />
                </span>
              </div>

              {/* Info */}
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="font-medium text-gray-200 text-sm truncate">{conv.customerId.slice(0, 8)}…</p>
                  {conv.status === "WAITING_HUMAN" && (
                    <span className="text-xs px-2 py-0.5 rounded-full bg-red-500/15 text-red-400 border border-red-500/20 flex-shrink-0">
                      ⚠ Requiere atención
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 mt-0.5">
                  {conv.status === "AI_ACTIVE" ? (
                    <Bot className="w-3 h-3 text-emerald-400 flex-shrink-0" />
                  ) : (
                    <User className="w-3 h-3 text-blue-400 flex-shrink-0" />
                  )}
                  <p className="text-xs text-gray-500 truncate">
                    {conv.escalationReason ?? conv.channel}
                  </p>
                </div>
              </div>

              {/* Right */}
              <div className="flex flex-col items-end gap-2 flex-shrink-0">
                <StatusBadge status={conv.status} />
                <p className="text-xs text-gray-500">{formatRelative(conv.lastMessageAt)}</p>
              </div>

              {/* Quick actions */}
              {conv.status === "WAITING_HUMAN" && (
                <button
                  onClick={(e) => { e.preventDefault(); takeOver(conv.id); }}
                  className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium transition-colors flex-shrink-0"
                >
                  Tomar control
                </button>
              )}
              {conv.status === "HUMAN_ACTIVE" && (
                <button
                  onClick={(e) => { e.preventDefault(); resolve(conv.id); }}
                  className="p-1.5 rounded-lg hover:bg-emerald-500/10 text-gray-500 hover:text-emerald-400 transition-colors flex-shrink-0"
                  title="Resolver"
                >
                  <CheckCheck className="w-4 h-4" />
                </button>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
