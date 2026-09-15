"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  appointmentsApi, conversationsApi, servicesApi,
  employeesApi, customersApi, reportsApi, businessApi,
} from "@/lib/api/endpoints";
import type { AppointmentStatus } from "@/types";
import { ApiError } from "@/lib/api/client";

// Helper: treat 404 as "not found" (returns undefined) instead of an error
const notFoundAsNull = async <T>(fn: () => Promise<T>): Promise<T | null> => {
  try {
    return await fn();
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
};

// ---- Business ----
export function useBusiness() {
  return useQuery({ queryKey: ["business"], queryFn: businessApi.get });
}
export function useBusinessConfig() {
  return useQuery({
    queryKey: ["business-config"],
    queryFn: () => notFoundAsNull(businessApi.getConfig),
    retry: false,
  });
}
export function useBusinessHours() {
  return useQuery({
    queryKey: ["business-hours"],
    queryFn: () => notFoundAsNull(businessApi.getHours),
    retry: false,
  });
}

// ---- Services ----
export function useServices() {
  return useQuery({ queryKey: ["services"], queryFn: servicesApi.list });
}
export function useDeleteService() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: servicesApi.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["services"] }),
  });
}
export function useCreateService() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: servicesApi.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["services"] }),
  });
}
export function useUpdateService() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => servicesApi.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["services"] }),
  });
}

// ---- Employees ----
export function useEmployees() {
  return useQuery({ queryKey: ["employees"], queryFn: employeesApi.list });
}

// ---- Customers ----
export function useCustomers(page = 0, size = 20, search?: string) {
  return useQuery({
    queryKey: ["customers", page, size, search],
    queryFn: () => customersApi.list(page, size, search),
  });
}

// ---- Appointments ----
export function useAppointments(params?: {
  page?: number; size?: number; status?: AppointmentStatus;
  dateFrom?: string; dateTo?: string;
}) {
  return useQuery({
    queryKey: ["appointments", params],
    queryFn: () => appointmentsApi.list(params),
  });
}
export function useAvailability(date: string, serviceId: string, employeeId?: string) {
  return useQuery({
    queryKey: ["availability", date, serviceId, employeeId],
    queryFn: () => appointmentsApi.availability(date, serviceId, employeeId),
    enabled: !!date && !!serviceId,
  });
}
export function useCancelAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      appointmentsApi.cancel(id, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments"] }),
  });
}
export function useUpdateAppointmentStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status, reason }: { id: string; status: AppointmentStatus; reason?: string }) =>
      appointmentsApi.updateStatus(id, status, reason),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments"] }),
  });
}

// ---- Conversations ----
export function useConversations() {
  return useQuery({
    queryKey: ["conversations"],
    queryFn: conversationsApi.list,
    refetchInterval: 15_000, // Poll every 15s
  });
}
export function useConversation(id: string) {
  return useQuery({
    queryKey: ["conversation", id],
    queryFn: () => conversationsApi.get(id),
    enabled: !!id,
    refetchInterval: 8_000,
  });
}
export function useMessages(conversationId: string) {
  return useQuery({
    queryKey: ["messages", conversationId],
    queryFn: () => conversationsApi.getMessages(conversationId),
    enabled: !!conversationId,
    refetchInterval: 5_000,
  });
}
export function useSendReply() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, content }: { id: string; content: string }) =>
      conversationsApi.sendReply(id, content),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["messages", vars.id] });
      qc.invalidateQueries({ queryKey: ["conversations"] });
    },
  });
}
export function useTakeOver() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: conversationsApi.takeOver,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["conversations"] });
      qc.invalidateQueries({ queryKey: ["conversation"] });
    },
  });
}
export function useReleaseToAi() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: conversationsApi.release,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["conversations"] });
      qc.invalidateQueries({ queryKey: ["conversation"] });
    },
  });
}
export function useResolveConversation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: conversationsApi.resolve,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["conversations"] }),
  });
}

// ---- Reports ----
export function useLatestReport() {
  return useQuery({
    queryKey: ["report-latest"],
    queryFn: () => notFoundAsNull(reportsApi.latest),
    retry: false,
    staleTime: 5 * 60 * 1000, // 5 min — reports don't change that fast
  });
}
export function useReports() {
  return useQuery({ queryKey: ["reports"], queryFn: () => reportsApi.list() });
}
