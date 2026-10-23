import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "../features/auth/api";
import { activateStomp, deactivateStomp } from "../lib/stompClient";
import type { User } from "../types/user";
import { useToast } from "./ToastContext";

interface AuthContextData {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextData | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(false);

  const { success, error } = useToast();

  useEffect(() => {
    const verifySession = async () => {
      setLoading(true);

      try {
        const response = await authApi.me();
        setUser(response);
        activateStomp();
      } catch (error) {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    verifySession();
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<void> => {
    setLoading(true);

    try {
      await authApi.login(email, password);

      const response = await authApi.me();
      setUser(response);
      activateStomp();

      success("Login realizado com sucesso.", {
        id: "auth-success",
        duration: 2000,
      });
    } catch (err) {
      console.error("Erro ao realizar login: ", err);

      error("Erro ao realizar login.", {
        id: "auth-error",
        duration: 2000,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    setLoading(true);

    try {
      await authApi.logout();
      setUser(null);
      deactivateStomp();

      success("Logout realizado com sucesso", {
        id: "auth-success",
        duration: 2000,
      });
    } catch (err) {
      console.error("Erro ao realizar logout: ", err);
      error("Erro ao realizar logout", {
        id: "auth-error",
        duration: 2000,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async (name: string, email: string, password: string): Promise<void> => {
    setLoading(true);

    try {
      await authApi.register(name, email, password);

      const response = await authApi.me();
      setUser(response);

      success("Registro realizado com sucesso", {
        id: "auth-success",
        duration: 2000,
      });
    } catch (err) {
      console.error("Erro ao realizar registro: ", err);
      error("Erro ao registrar nova conta", {
        id: "auth-error",
        duration: 2000,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshUser = useCallback(async (): Promise<void> => {
    setLoading(true);

    try {
      const response = await authApi.me();
      setUser(response);
      activateStomp();
    } catch (err) {
      console.error("Erro ao atualizar dados do usuário:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, logout, register, refreshUser }),
    [user, loading, login, logout, register, refreshUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextData {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error("useAuth precisa ser usado dentro de um <AuthProvider>");
  }

  return context;
}
