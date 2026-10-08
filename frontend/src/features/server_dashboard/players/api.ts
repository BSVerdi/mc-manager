import { http } from "../../../lib/apiClient";
import type { Player } from "./types";

export const playersApi = {
  getPlayers: (serverId: number) => http.get<Player[]>(`/servers/${serverId}/players`).then((res) => res.data),

  opPlayer: (serverId: number, nickname: string) =>
    http.patch<void>(`/servers/${serverId}/players/${encodeURIComponent(nickname)}/op`).then((res) => res.data),

  deopPlayer: (serverId: number, nickname: string) =>
    http.patch(`/servers/${serverId}/players/${encodeURIComponent(nickname)}/deop`).then((res) => res.data),

  ban: (serverId: number, nickname: string) =>
    http.patch(`/servers/${serverId}/players/${encodeURIComponent(nickname)}/ban`).then((res) => res.data),

  unban: (serverId: number, nickname: string) =>
    http.patch(`/servers/${serverId}/players/${encodeURIComponent(nickname)}/unban`).then((res) => res.data),
};
