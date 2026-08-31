import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { FileContent, FileEntry } from "../types/File";
import type { Player } from "../types/Player";
import {
  type CreateServerRequest,
  type Server,
  type ServerVersion,
  type UpdateServerRequest,
} from "../types/Server";
import type { Status } from "../types/Status";

const BASE_URL = import.meta.env.VITE_API_URL;

export const http = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

export const api = {
  servers: {
    list: () => http.get<Server[]>("/servers").then((res) => res.data),
    get: (serverId: number) =>
      http.get<Server>(`/servers/${serverId}`).then((res) => res.data),
    create: (data: CreateServerRequest) =>
      http.post<Server>("/servers", data).then((res) => res.data),
    update: (serverId: number, data: UpdateServerRequest) =>
      http.put<Server>(`/servers/${serverId}`, data).then((res) => res.data),
    delete: (serverId: number) =>
      http.delete<void>(`/servers/${serverId}`).then((res) => res.data),
    getReleasedVersions: () =>
      http
        .get<ServerVersion[]>("/servers/versions/released")
        .then((res) => res.data),
  },

  server: {
    start: (serverId: number) =>
      http.post<void>(`/servers/${serverId}/start`).then((res) => res.data),

    stop: (serverId: number) =>
      http.post<void>(`/servers/${serverId}/stop`).then((res) => res.data),

    restart: (serverId: number) =>
      http.post<void>(`/servers/${serverId}/restart`).then((res) => res.data),

    status: (serverId: number) =>
      http
        .get<{ status: Status }>(`/servers/${serverId}/status`)
        .then((res) => res.data),
  },

  player: {
    getPlayers: (serverId: number) =>
      http
        .get<Player[]>(`/servers/${serverId}/players`)
        .then((res) => res.data),

    opPlayer: (serverId: number, nickname: string) =>
      http
        .patch<void>(
          `/servers/${serverId}/players/${encodeURIComponent(nickname)}/op`,
        )
        .then((res) => res.data),

    deopPlayer: (serverId: number, nickname: string) =>
      http
        .patch(
          `/servers/${serverId}/players/${encodeURIComponent(nickname)}/deop`,
        )
        .then((res) => res.data),

    ban: (serverId: number, nickname: string) =>
      http
        .patch(
          `/servers/${serverId}/players/${encodeURIComponent(nickname)}/ban`,
        )
        .then((res) => res.data),

    unban: (serverId: number, nickname: string) =>
      http
        .patch(
          `/servers/${serverId}/players/${encodeURIComponent(nickname)}/unban`,
        )
        .then((res) => res.data),
  },

  files: {
    list: (serverId: number, path = "") =>
      http
        .get<FileEntry[]>(`/servers/${serverId}/files`, { params: { path } })
        .then((res) => res.data),

    getContent: (serverId: number, path: string) =>
      http
        .get<FileContent>(`/servers/${serverId}/files/content`, {
          params: { path },
        })
        .then((res) => res.data),

    saveContent: (serverId: number, path: string, content: string) =>
      http
        .put<void>(
          `/servers/${serverId}/files/content`,
          { content },
          { params: { path } },
        )
        .then((res) => res.data),

    downloadUrl: (serverId: number, path: string) =>
      `${BASE_URL}/servers/${serverId}/files/download?path=${encodeURIComponent(path)}`,

    downloadZipUrl: (serverId: number, path: string) =>
      `${BASE_URL}/servers/${serverId}/files/download-zip?path=${encodeURIComponent(path)}`,

    upload: (serverId: number, path: string, file: File) => {
      const formData = new FormData();
      formData.append("file", file);

      return http
        .post<void>(`/servers/${serverId}/files/upload`, formData, {
          params: { path },
          headers: { "Content-Type": "multipart/form-data" },
        })
        .then((res) => res.data);
    },

    uploadZip: (serverId: number, path: string, zipFile: File) => {
      const formData = new FormData();
      formData.append("file", zipFile);

      return http
        .post<void>(`/servers/${serverId}/files/upload-zip`, formData, {
          params: { path },
          headers: { "Content-Type": "multipart/form-data" },
        })
        .then((res) => res.data);
    },

    delete: (serverId: number, path: string) =>
      http
        .delete<void>(`/servers/${serverId}/files/delete`, {
          params: { path },
        })
        .then((res) => res.data),
  },

  auth: {
    login: async (email: string, password: string) =>
      http.post("/auth/login", { email, password }).then((res) => res.data),

    logout: async () => http.post("/auth/logout").then((res) => res.data),

    register: async (name: string, email: string, password: string) =>
      http
        .post("/auth/register", { name, email, password })
        .then((res) => res.data),

    me: async () => http.get("/auth/me").then((res) => res.data),
  },

  user: {
    update: async (userId: number, username: string) =>
      http
        .patch(`/users/${userId}/update-user`, { username })
        .then((res) => res.data),

    updateProfilePic: async (userId: number, file: File) => {
      const formData = new FormData();
      formData.append("file", file);

      return http
        .patch(`/users/${userId}/profile-picture`, formData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        })
        .then((res) => res.data);
    },

    delete: async (userId: number) =>
      http.delete(`/users/${userId}/delete`).then((res) => res.data),
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

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry
    ) {
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
