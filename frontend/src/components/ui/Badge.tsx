import { cn, getStatusColor, getStatusLabel } from "@/lib/utils";

interface BadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border",
        getStatusColor(status),
        className
      )}
    >
      {getStatusLabel(status)}
    </span>
  );
}

interface ConversationStatusDotProps {
  status: string;
}

export function ConversationStatusDot({ status }: ConversationStatusDotProps) {
  const cls = {
    AI_ACTIVE:     "status-dot ai",
    WAITING_HUMAN: "status-dot wait",
    HUMAN_ACTIVE:  "status-dot human",
    RESOLVED:      "status-dot resolved",
  }[status] ?? "status-dot resolved";

  return <span className={cls} />;
}
