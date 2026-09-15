import { api } from "@/lib/api/client";
import type {
  Appointment, AppointmentStatus, AvailableSlot,
  Customer, Service, Employee,
  Conversation, Message,
  Report, PageResponse, Business, BusinessHours,
} from "@/types";

// ---- Auth ----
export const authApi = {
  login: (email: string, password: string) =>
    api.post<any>("/auth/login", { email, password }),
  register: (data: any) =>
    api.post<any>("/auth/register", data),
};

// ---- Business ----
export const businessApi = {
  get: () => api.get<Business>("/business"),
  update: (data: Partial<Business>) => api.put<Business>("/business", data),
  getConfig: () => api.get<any>("/business/configuration"),
  updateConfig: (data: any) => api.put<any>("/business/configuration", data),
  getHours: () => api.get<BusinessHours[]>("/business/hours"),
  updateHours: (hours: BusinessHours[]) => api.put<BusinessHours[]>("/business/hours", hours),
};

// ---- Services ----
export const servicesApi = {
  list: () => api.get<Service[]>("/services"),
  get: (id: string) => api.get<Service>(`/services/${id}`),
  create: (data: any) => api.post<Service>("/services", data),
  update: (id: string, data: any) => api.put<Service>(`/services/${id}`, data),
  delete: (id: string) => api.delete(`/services/${id}`),
};

// ---- Employees ----
export const employeesApi = {
  list: () => api.get<Employee[]>("/employees"),
  get: (id: string) => api.get<Employee>(`/employees/${id}`),
  create: (data: any) => api.post<Employee>("/employees", data),
  update: (id: string, data: any) => api.put<Employee>(`/employees/${id}`, data),
  delete: (id: string) => api.delete(`/employees/${id}`),
};

// ---- Customers ----
export const customersApi = {
  list: (page = 0, size = 20, search?: string) => {
    const q = new URLSearchParams({ page: String(page), size: String(size) });
    if (search) q.set("search", search);
    return api.get<PageResponse<Customer>>(`/customers?${q}`);
  },
  get: (id: string) => api.get<Customer>(`/customers/${id}`),
  update: (id: string, data: any) => api.put<Customer>(`/customers/${id}`, data),
};

// ---- Appointments ----
export const appointmentsApi = {
  list: (params?: {
    page?: number; size?: number; status?: AppointmentStatus;
    employeeId?: string; dateFrom?: string; dateTo?: string;
  }) => {
    const q = new URLSearchParams();
    if (params?.page !== undefined) q.set("page", String(params.page));
    if (params?.size !== undefined) q.set("size", String(params.size));
    if (params?.status) q.set("status", params.status);
    if (params?.employeeId) q.set("employeeId", params.employeeId);
    if (params?.dateFrom) q.set("dateFrom", params.dateFrom);
    if (params?.dateTo) q.set("dateTo", params.dateTo);
    return api.get<PageResponse<Appointment>>(`/appointments?${q}`);
  },
  get: (id: string) => api.get<Appointment>(`/appointments/${id}`),
  create: (data: any) => api.post<Appointment>("/appointments", data),
  updateStatus: (id: string, status: AppointmentStatus, reason?: string) =>
    api.patch<Appointment>(`/appointments/${id}/status`, { status, reason }),
  cancel: (id: string, reason?: string) =>
    api.delete(`/appointments/${id}${reason ? `?reason=${encodeURIComponent(reason)}` : ""}`),
  availability: (date: string, serviceId: string, employeeId?: string) => {
    const q = new URLSearchParams({ date, serviceId });
    if (employeeId) q.set("employeeId", employeeId);
    return api.get<AvailableSlot[]>(`/appointments/availability?${q}`);
  },
};

// ---- Conversations ----
export const conversationsApi = {
  list: () => api.get<Conversation[]>("/conversations"),
  get: (id: string) => api.get<Conversation>(`/conversations/${id}`),
  getMessages: (id: string) => api.get<Message[]>(`/conversations/${id}/messages`),
  sendReply: (id: string, content: string) =>
    api.post<Message>(`/conversations/${id}/messages`, { content }),
  takeOver: (id: string) => api.post<Conversation>(`/conversations/${id}/take-over`),
  release: (id: string) => api.post<Conversation>(`/conversations/${id}/release`),
  resolve: (id: string) => api.post<Conversation>(`/conversations/${id}/resolve`),
};

// ---- WhatsApp Integration ----
export const whatsappApi = {
  getStatus: () => api.get<any>("/whatsapp/integration"),
  connect: (data: {
    phoneNumberId: string;
    accessToken: string;
    whatsappBusinessAccountId?: string;
    displayPhone?: string;
  }) => api.post<any>("/whatsapp/integration/connect", data),
  disconnect: () => api.delete("/whatsapp/integration"),
  test: (toPhone: string) =>
    api.post<any>(`/whatsapp/integration/test?toPhone=${encodeURIComponent(toPhone)}`),
};

// ---- Reports ----
export const reportsApi = {
  list: (limit = 10) => api.get<Report[]>(`/reports?limit=${limit}`),
  latest: () => api.get<Report>("/reports/latest"),
  generate: () => api.post<Report>("/reports/generate"),
};
