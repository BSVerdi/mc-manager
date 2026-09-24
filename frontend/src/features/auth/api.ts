import { http } from "../../lib/apiClient";

export const authApi = {
  login: async (email: string, password: string) =>
    http.post("/auth/login", { email, password }).then((res) => res.data),

  logout: async () => http.post("/auth/logout").then((res) => res.data),

  register: async (name: string, email: string, password: string) =>
    http.post("/auth/register", { name, email, password }).then((res) => res.data),

  me: async () => http.get("/auth/me").then((res) => res.data),
};
