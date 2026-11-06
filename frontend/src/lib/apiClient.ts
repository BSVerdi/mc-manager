import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { type Catalog, type Server } from "../types/server";
import type { Status } from "../types/status";
import { deactivateStomp } from "./stompClient";

export const BASE_URL = import.meta.env.VITE_API_URL;

const clientConfig = {
  baseURL: BASE_URL,
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
};

export const http = axios.create(clientConfig);

export const api = {
  servers: {
    list: () => http.get<Server[]>("/servers").then((res) => res.data),

    get: (serverId: number) => http.get<Server>(`/servers/${serverId}`).then((res) => res.data),

    getCatalog: () => http.get<Catalog[]>("/servers/catalog").then((res) => res.data),
  },

  server: {
    start: (serverId: number) => http.post<void>(`/servers/${serverId}/start`).then((res) => res.data),

    stop: (serverId: number) => http.post<void>(`/servers/${serverId}/stop`).then((res) => res.data),

    restart: (serverId: number) => http.post<void>(`/servers/${serverId}/restart`).then((res) => res.data),

    status: (serverId: number) => http.get<{ status: Status }>(`/servers/${serverId}/status`).then((res) => res.data),
  },
};

const refreshClient = axios.create(clientConfig);

const NO_REFRESH_URLS = ["/auth/login", "/auth/register", "/auth/refresh-token", "/auth/logout"];

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

let refreshPromise: Promise<void> | null = null;

const refreshSession = () => {
  refreshPromise ??= refreshClient
    .post("/auth/refresh-token")
    .then(() => undefined)
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
};

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryableRequestConfig | undefined;
    const skip = NO_REFRESH_URLS.some((u) => original?.url?.includes(u));

    if (error.response?.status !== 401 || !original || original._retry || skip) {
      return Promise.reject(error);
    }

    original._retry = true;

    try {
      await refreshSession();
    } catch (refreshError) {
      deactivateStomp();
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
      return Promise.reject(refreshError);
    }

    return http(original);
  },
);
