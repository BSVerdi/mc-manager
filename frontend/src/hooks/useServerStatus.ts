import { useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import { Status } from "../types/Status";

export function useServerStatus(serverId: number) {
  const [status, setStatus] = useState<Status>(Status.OFFLINE);

  useEffect(() => {
    api.server
      .status(serverId)
      .then((res) => setStatus(res.status))
      .catch((err) =>
        console.error(
          "[useServerStatus] Erro ao carregar o status do servidor.",
          err,
        ),
      );
  }, [serverId]);

  console.log(status);

  return { status, setStatus };
}
