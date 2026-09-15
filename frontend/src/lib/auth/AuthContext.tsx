"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, getRefreshToken } from "@/lib/api/client";
import type { AuthResponse } from "@/types";

interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

interface AuthBusiness {
  id: string;
  name: string;
  slug: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  business: AuthBusiness | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
}

interface RegisterData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  businessName: string;
  businessType?: string;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [business, setBusiness] = useState<AuthBusiness | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    // Restore session from stored tokens, then validate the token is still alive
    const token = localStorage.getItem("accessToken");
    const storedUser = localStorage.getItem("authUser");
    const storedBusiness = localStorage.getItem("authBusiness");

    if (token && storedUser && storedBusiness) {
      // Optimistically restore session first (fast UI)
      setUser(JSON.parse(storedUser));
      setBusiness(JSON.parse(storedBusiness));
      // Then validate the token silently — if 401, client.ts will auto-clear and redirect
      import("@/lib/api/endpoints")
        .then(({ businessApi }) => businessApi.get())
        .catch(() => {
          // 401 is handled in client.ts (clears localStorage + redirects to /login)
          // Other errors (network, 500) are ignored — don't kick the user out
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  const storeSession = (data: AuthResponse) => {
    // localStorage for client-side reads
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);
    localStorage.setItem("authUser", JSON.stringify(data.user));
    localStorage.setItem("authBusiness", JSON.stringify(data.business));
    // Cookie for server-side proxy auth check (httpOnly=false so JS can still clear it)
    document.cookie = `accessToken=${data.accessToken}; path=/; max-age=900; SameSite=Lax`;
    setUser(data.user);
    setBusiness(data.business);
  };

  const login = async (email: string, password: string) => {
    const data = await api.post<AuthResponse>("/auth/login", { email, password });
    storeSession(data);
    router.push("/dashboard");
  };

  const register = async (formData: RegisterData) => {
    const data = await api.post<AuthResponse>("/auth/register", formData);
    storeSession(data);
    router.push("/dashboard");
  };

  const logout = async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) {
        await api.post(`/auth/logout?token=${refreshToken}`);
      }
    } catch {}
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("authUser");
    localStorage.removeItem("authBusiness");
    // Clear the cookie
    document.cookie = "accessToken=; path=/; max-age=0";
    setUser(null);
    setBusiness(null);
    router.push("/login");
  };

  return (
    <AuthContext.Provider value={{ user, business, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
