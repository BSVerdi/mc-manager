import axios from "axios";
import type { Status } from "../types/status";
import type { FileContent, FileEntry } from "../types/file";

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

export const api = {
  server: {
    start: () => http.post<void>("/server/start").then((res) => res.data),
    stop: () => http.post<void>("/server/stop").then((res) => res.data),
    restart: () => http.post<void>("/server/restart").then((res) => res.data),
    status: () =>
      http.get<{ status: Status }>("/server/status").then((res) => res.data),
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
  },
};

// export const startServer = async () => {
//   const res = await api.post("/start");

//   return res.data;
// };

// export const stopServer = async () => {
//   const res = await api.post("/stop");

//   return res.data;
// };

// export const restartServer = async () => {
//   const res = await api.post("/restart");

//   return res.data;
// };

// export const getStatus = async () => {
//   const res = await api.get("/status");

//   return res.data;
// };

// export const getFiles = async () => {};
