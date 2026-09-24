import { http } from "../../lib/apiClient";
import type { Server } from "../../types/server";
import type { ServerVersion } from "../server_dashboard/types";
import type { CreateServerRequest } from "./types";

export const createServerApi = {
  create: (data: CreateServerRequest) => http.post<Server>("/servers", data).then((res) => res.data),

  getReleasedVersions: () => http.get<ServerVersion[]>("/servers/versions/released").then((res) => res.data),
};
