import { http } from "../../../lib/apiClient";
import type { Server, UpdateServerRequest } from "../types";

export const serverSettingsApi = {
  update: (serverId: number, data: UpdateServerRequest) =>
    http.put<Server>(`/servers/${serverId}`, data).then((res) => res.data),

  delete: (serverId: number) => http.delete<void>(`/servers/${serverId}`).then((res) => res.data),
};
