import axios from "axios";
import type { Status } from "../types/status";
import type { FileContent, FileEntry } from "../types/file";
import type { Player } from "../types/player";

const BASE_URL = import.meta.env.VITE_API_URL;

export const http = axios.create({
  baseURL: BASE_URL,
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
};
