import { http } from "../../../lib/apiClient";
import type { FileContent, FileEntry } from "./types";

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");

  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();

  requestAnimationFrame(() => {
    a.remove();
    URL.revokeObjectURL(url);
  });
}

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

  download: (serverId: number, path: string, filename: string) =>
    http
      .get<Blob>(`/servers/${serverId}/files/download`, {
        params: { path },
        responseType: "blob",
      })
      .then((res) => triggerDownload(res.data, filename)),

  downloadZip: (serverId: number, path: string, folderName: string) =>
    http
      .get<Blob>(`/servers/${serverId}/files/download-zip`, {
        params: { path },
        responseType: "blob",
      })
      .then((res) => triggerDownload(res.data, `${folderName}.zip`)),

  upload: (serverId: number, path: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    return http
      .post<void>(`/servers/${serverId}/files/upload`, formData, {
        params: { path },
      })
      .then((res) => res.data);
  },

  uploadZip: (serverId: number, path: string, zipFile: File) => {
    const formData = new FormData();
    formData.append("file", zipFile);

    return http
      .post<void>(`/servers/${serverId}/files/upload-zip`, formData, {
        params: { path },
      })
      .then((res) => res.data);
  },

  delete: (serverId: number, path: string) =>
    http.delete<void>(`/servers/${serverId}/files/delete`, { params: { path } }).then((res) => res.data),
};
