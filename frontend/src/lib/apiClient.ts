import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { Status } from "../types/Status";
import type { FileContent, FileEntry } from "../types/File";
import type { Player } from "../types/Player";

const BASE_URL = import.meta.env.VITE_API_URL;

export const http = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

export const api = {
  server: {
    start: () => http.post<void>("/server/start").then((res) => res.data),

    stop: () => http.post<void>("/server/stop").then((res) => res.data),

    restart: () => http.post<void>("/server/restart").then((res) => res.data),

    status: () =>
      http.get<{ status: Status }>("/server/status").then((res) => res.data),

    getPlayers: () =>
      http.get<Player[]>("/server/players").then((res) => res.data),

    opPlayer: (nickname: string) =>
      http
        .patch<void>("/server/players/op", null, { params: { nickname } })
        .then((res) => res.data),

    deopPlayer: (nickname: string) =>
      http
        .patch<void>("/server/players/deop", null, { params: { nickname } })
        .then((res) => res.data),
  },

  files: {
    list: (path = "") =>
      http
        .get<FileEntry[]>("/files", { params: { path } })
        .then((res) => res.data),

    getContent: (path: string) =>
      http
        .get<FileContent>("/files/content", { params: { path } })
        .then((res) => res.data),

    saveContent: (path: string, content: string) =>
      http
        .put<void>("/files/content", { content }, { params: { path } })
        .then((res) => res.data),

    downloadUrl: (path: string) =>
      `${BASE_URL}/files/download?path=${encodeURIComponent(path)}`,

    downloadZipUrl: (path: string) =>
      `${BASE_URL}/files/download-zip?path=${encodeURIComponent(path)}`,

    upload: (path: string, file: File) => {
      const formData = new FormData();
      formData.append("file", file);

      return http
        .post<void>("/files/upload", formData, {
          params: { path },
          headers: { "Content-Type": "multipart/form-data" },
        })
        .then((res) => res.data);
    },
  },

  auth: {
    login: async (email: string, password: string) =>
      http.post("/auth/login", { email, password }).then((res) => res.data),

    logout: async () => http.post("/auth/logout").then((res) => res.data),

    me: async () => http.get("/auth/me").then((res) => res.data),
  },
};

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

let isRefreshing = false;
let pendingRequests: Array<() => void> = [];

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry
    ) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push(() => resolve(http(originalRequest)));
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await http.post("/auth/refresh-token");

        pendingRequests.forEach((retry) => retry());
        pendingRequests = [];

        return http(originalRequest);
      } catch (refreshError) {
        pendingRequests = [];
        window.location.href = "/login";

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);
