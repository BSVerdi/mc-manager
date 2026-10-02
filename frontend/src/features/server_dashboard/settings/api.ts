import type { Server } from "http";
import { http } from "../../../lib/apiClient";
import type { UpdateServerRequest } from "../types";

export const serverSettingsApi = {
  update: (serverId: number, data: UpdateServerRequest) =>
    http.put<Server>(`/servers/${serverId}`, data).then((res) => res.data),

  delete: (serverId: number) => http.delete<void>(`/servers/${serverId}`).then((res) => res.data),
};
