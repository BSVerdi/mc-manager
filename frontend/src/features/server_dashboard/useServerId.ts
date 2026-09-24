import { useOutletContext } from "react-router-dom";
import type { DashboardContext } from "./ServerDashboard";

export function useServerId(): number {
  return useOutletContext<DashboardContext>().serverId;
}
