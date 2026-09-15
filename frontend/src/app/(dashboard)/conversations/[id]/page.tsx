"use client";

import { use, useState, useEffect, useRef } from "react";
import { useConversation, useMessages, useSendReply, useTakeOver, useReleaseToAi, useResolveConversation } from "@/lib/api/hooks";
import { PageLoader, Button } from "@/components/ui/Shared";
import { StatusBadge } from "@/components/ui/Badge";
import { formatDateTime } from "@/lib/utils";
import { ArrowLeft, Bot, User, Send, RefreshCw, CheckCheck, UserCheck } from "lucide-react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import type { Message } from "@/types";

export default function ConversationDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [reply, setReply] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  const { data: conversation, isLoading: convLoading } = useConversation(id);
  const { data: messages, isLoading: msgLoading } = useMessages(id);
  const { mutate: sendReply, isPending: sending } = useSendReply();
  const { mutate: takeOver, isPending: takingOver } = useTakeOver();
  const { mutate: release } = useReleaseToAi();
  const { mutate: resolve } = useResolveConversation();

  // Auto-scroll to bottom
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = () => {
    if (!reply.trim() || sending) return;
    sendReply({ id, content: reply.trim() });
    setReply("");
  };

  if (convLoading || msgLoading) return <PageLoader />;
  if (!conversation) return <p className="text-gray-400">Conversación no encontrada</p>;

  const canReply = conversation.status !== "RESOLVED";

  return (
    <div className="fade-in flex flex-col h-[calc(100vh-4rem)]">
      {/* Header */}
      <div className="flex items-center gap-4 mb-5 pb-5 border-b border-gray-800/50 flex-shrink-0">
        <Link href="/conversations" className="p-2 rounded-xl hover:bg-gray-800/50 text-gray-400 hover:text-gray-200 transition-colors">
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3">
            <p className="font-semibold text-gray-100 text-sm">
              Cliente: {conversation.customerId.slice(0, 8)}…
            </p>
            <StatusBadge status={conversation.status} />
          </div>
          <p className="text-xs text-gray-500 mt-0.5">Canal: {conversation.channel}</p>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 flex-shrink-0">
          {conversation.status === "WAITING_HUMAN" && (
            <Button variant="primary" size="sm" onClick={() => takeOver(id)} disabled={takingOver}>
              <UserCheck className="w-3.5 h-3.5" />
              Tomar control
            </Button>
          )}
          {conversation.status === "HUMAN_ACTIVE" && (
            <>
              <Button variant="secondary" size="sm" onClick={() => release(id)}>
                <Bot className="w-3.5 h-3.5" />
                Devolver a IA
              </Button>
              <Button variant="secondary" size="sm" onClick={() => resolve(id)}>
                <CheckCheck className="w-3.5 h-3.5" />
                Resolver
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto space-y-3 pb-4">
        {messages?.length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">Sin mensajes aún</p>
        )}
        {messages?.map((msg) => (
          <MessageBubble key={msg.id} msg={msg} />
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Reply input */}
      {canReply && (
        <div className="flex-shrink-0 pt-4 border-t border-gray-800/50">
          {conversation.status === "AI_ACTIVE" && (
            <p className="text-xs text-amber-400 mb-2 flex items-center gap-1.5">
              <Bot className="w-3 h-3" />
              La IA está gestionando esta conversación. Tu respuesta la pondrá en modo humano.
            </p>
          )}
          <div className="flex gap-2">
            <input
              type="text"
              value={reply}
              onChange={(e) => setReply(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleSend()}
              placeholder="Escribe tu respuesta..."
              className="flex-1 px-4 py-2.5 rounded-xl bg-gray-900/60 border border-gray-700/50 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500 text-sm"
            />
            <Button variant="primary" onClick={handleSend} disabled={!reply.trim() || sending}>
              <Send className="w-4 h-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function MessageBubble({ msg }: { msg: Message }) {
  const isInbound = msg.direction === "INBOUND";
  const isAI = msg.senderType === "AI";

  return (
    <div className={cn("flex gap-2.5", isInbound ? "justify-start" : "justify-end")}>
      {isInbound && (
        <div className="w-7 h-7 rounded-full bg-gray-700 flex items-center justify-center flex-shrink-0 mt-1">
          <User className="w-3.5 h-3.5 text-gray-400" />
        </div>
      )}
      <div className={cn("max-w-[72%] space-y-1")}>
        <div
          className={cn(
            "px-4 py-2.5 rounded-2xl text-sm leading-relaxed",
            isInbound
              ? "bg-gray-800/60 text-gray-200 rounded-tl-sm"
              : isAI
              ? "bg-indigo-600/80 text-white rounded-tr-sm"
              : "bg-violet-600/80 text-white rounded-tr-sm"
          )}
        >
          {msg.content}
        </div>
        <div className={cn("flex items-center gap-1.5 px-1", isInbound ? "" : "justify-end")}>
          {!isInbound && (
            <>
              {isAI ? (
                <Bot className="w-3 h-3 text-indigo-400" />
              ) : (
                <User className="w-3 h-3 text-violet-400" />
              )}
              <span className="text-xs text-gray-500">{isAI ? "IA" : "Humano"}</span>
            </>
          )}
          <span className="text-xs text-gray-600">{formatDateTime(msg.createdAt)}</span>
        </div>
      </div>
      {!isInbound && (
        <div className={cn(
          "w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 mt-1",
          isAI ? "bg-indigo-500/20" : "bg-violet-500/20"
        )}>
          {isAI ? <Bot className="w-3.5 h-3.5 text-indigo-400" /> : <User className="w-3.5 h-3.5 text-violet-400" />}
        </div>
      )}
    </div>
  );
}
