import { BASE_URL, http } from "../../../lib/apiClient";
import type { FileContent, FileEntry } from "./types";

export const filesApi = {
  list: (serverId: number, path = "") =>
    http.get<FileEntry[]>(`/servers/${serverId}/files`, { params: { path } }).then((res) => res.data),

  getContent: (serverId: number, path: string) =>
    http
      .get<FileContent>(`/servers/${serverId}/files/content`, {
        params: { path },
      })
      .then((res) => res.data),

  saveContent: (serverId: number, path: string, content: string) =>
    http.put<void>(`/servers/${serverId}/files/content`, { content }, { params: { path } }).then((res) => res.data),

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
};
