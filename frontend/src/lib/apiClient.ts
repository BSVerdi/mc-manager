import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { type Catalog, type Server } from "../types/server";
import type { Status } from "../types/status";
import { deactivateStomp } from "./stompClient";

export const BASE_URL = import.meta.env.VITE_API_URL;

export const http = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

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

const refreshClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

type PendingRequest = {
  resolve: (value: unknown) => void;
  reject: (reason: unknown) => void;
  config: RetryableRequestConfig;
};

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

let isRefreshing = false;
let pendingRequests: PendingRequest[] = [];

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const isAuthEndpoint = originalRequest?.url?.includes("/auth/");

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        originalRequest._retry = true;

        return new Promise((resolve, reject) => {
          pendingRequests.push({ resolve, reject, config: originalRequest });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await refreshClient.post("/auth/refresh-token");

        pendingRequests.forEach(({ resolve, config }) => resolve(http(config)));
        pendingRequests = [];

        return http(originalRequest);
      } catch (refreshError) {
        pendingRequests.forEach(({ reject }) => reject(refreshError));
        pendingRequests = [];

        deactivateStomp();

        if (window.location.pathname !== "/login") {
          window.location.href = "/login";
        }

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);
