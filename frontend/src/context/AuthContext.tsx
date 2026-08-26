import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { User } from "../types/User";
import { api } from "../lib/apiClient";

interface AuthContextData {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;
  register: (name: string, email: string, password: string) => Promise<boolean>;
}

const AuthContext = createContext<AuthContextData | undefined>(undefined);

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const verifySession = async () => {
      try {
        const response = await api.auth.me();
        setUser(response);
      } catch (error) {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    verifySession();
  }, []);

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      await api.auth.login(email, password);

      const response = await api.auth.me();
      setUser(response);

      return true;
    } catch (error) {
      console.error("Erro ao realizar login: ", error);
      return false;
    }
  };

  const logout = async () => {
    try {
      await api.auth.logout();
    } catch (error) {
      console.error("Erro ao realizar logout: ", error);
    } finally {
      setUser(null);
    }
  };

  const register = async (
    name: string,
    email: string,
    password: string,
  ): Promise<boolean> => {
    try {
      await api.auth.register(name, email, password);

      const response = await api.auth.me();
      setUser(response);

      return true;
    } catch (error) {
      console.error("Erro ao realizar login: ", error);
      return false;
    }
  };

  const value = useMemo(
    () => ({ user, loading, login, logout, register }),
    [user, loading, login, logout, register],
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
