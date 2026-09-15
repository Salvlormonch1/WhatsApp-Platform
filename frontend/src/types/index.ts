// Shared TypeScript types mirroring the backend domain

export interface Business {
  id: string;
  name: string;
  slug: string;
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
  timezone: string;
  active: boolean;
  createdAt: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

export type UserRole = "OWNER" | "ADMIN" | "EMPLOYEE";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
  };
  business: {
    id: string;
    name: string;
    slug: string;
  };
}

export interface Service {
  id: string;
  businessId: string;
  name: string;
  description?: string;
  price: number;
  durationMinutes: number;
  active: boolean;
  createdAt: string;
}

export interface Employee {
  id: string;
  businessId: string;
  name: string;
  phone?: string;
  email?: string;
  bio?: string;
  avatarUrl?: string;
  active: boolean;
}

export interface Customer {
  id: string;
  businessId: string;
  phone: string;
  name?: string;
  email?: string;
  notes?: string;
  firstContactAt: string;
  lastContactAt: string;
}

export type AppointmentStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED"
  | "NO_SHOW";

export type AppointmentChannel = "WHATSAPP" | "MANUAL" | "WEB";

export interface Appointment {
  id: string;
  businessId: string;
  customerId: string;
  serviceId: string;
  employeeId?: string;
  scheduledAt: string;
  durationMinutes: number;
  status: AppointmentStatus;
  notes?: string;
  channel: AppointmentChannel;
  createdBy: "AI" | "HUMAN";
  cancelledReason?: string;
  createdAt: string;
}

export type ConversationStatus =
  | "AI_ACTIVE"
  | "WAITING_HUMAN"
  | "HUMAN_ACTIVE"
  | "RESOLVED";

export interface Conversation {
  id: string;
  businessId: string;
  customerId: string;
  status: ConversationStatus;
  assignedTo?: string;
  channel: string;
  escalationReason?: string;
  escalatedAt?: string;
  resolvedAt?: string;
  lastMessageAt: string;
  createdAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  direction: "INBOUND" | "OUTBOUND";
  senderType: "CUSTOMER" | "AI" | "HUMAN";
  content: string;
  status: "SENT" | "DELIVERED" | "READ" | "FAILED";
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface BusinessHours {
  dayOfWeek: number;
  openTime?: string;
  closeTime?: string;
  isClosed: boolean;
}

export interface AvailableSlot {
  startAt: string;
  endAt: string;
  localTime: string;
  isoDateTime: string;
}

export interface Report {
  id: string;
  businessId: string;
  reportType: string;
  periodStart: string;
  periodEnd: string;
  data: {
    appointments: {
      total: number;
      confirmed: number;
      cancelled: number;
      completed: number;
      no_show: number;
    };
    customers: { new: number };
    conversations: {
      total: number;
      escalated: number;
      escalation_rate: string;
    };
    top_service?: string;
  };
  sentAt?: string;
  createdAt: string;
}
