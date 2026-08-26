import { useOutletContext } from "react-router-dom";

type DashboardContext = { serverId: number };

export function useServerId(): number {
  return useOutletContext<DashboardContext>().serverId;
}
