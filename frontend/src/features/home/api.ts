import { http } from "../../lib/apiClient";
import type { Server } from "../../types/server";
import type { CreateServerRequest } from "./types";

export const createServerApi = {
  create: (data: CreateServerRequest) => http.post<Server>("/servers", data).then((res) => res.data),

  getReleasedVersions: (type: string) =>
    http.get<string[]>("/servers/versions/released", { params: { type } }).then((res) => res.data),
};
