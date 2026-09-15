// API client — typed wrapper around fetch
// All requests go through Next.js rewrite proxy (/api/backend → saas_backend:8080)
// This eliminates CORS because the browser only ever talks to localhost:3000.

const BASE_URL = "/api/backend";


export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public data?: unknown
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("accessToken");
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
}

export function clearTokens() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("refreshToken");
}

// Auth routes that must NEVER send a stored token (avoids 401 on expired tokens blocking public endpoints)
const AUTH_ROUTES = ["/auth/login", "/auth/register", "/auth/refresh"];

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const isAuthRoute = AUTH_ROUTES.some((r) => path.startsWith(r));

  const headers: HeadersInit = {
    "Content-Type": "application/json",
    // Don't send a stored token on auth routes — an expired token would cause 401 on public endpoints
    ...(!isAuthRoute && token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const body = await res.json().catch(() => null);

  if (!res.ok) {
    // 401: token is expired or invalid — clear session and redirect to login
    if (res.status === 401 && typeof window !== "undefined") {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("authUser");
      localStorage.removeItem("authBusiness");
      document.cookie = "accessToken=; path=/; max-age=0";
      // Only redirect if not already on an auth page
      if (!window.location.pathname.startsWith("/login") && !window.location.pathname.startsWith("/register")) {
        window.location.href = "/login";
      }
    }
    throw new ApiError(
      res.status,
      body?.message || `HTTP ${res.status}`,
      body
    );
  }

  return body?.data ?? body;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(data) }),
  put: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(data) }),
  patch: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "PATCH", body: JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
