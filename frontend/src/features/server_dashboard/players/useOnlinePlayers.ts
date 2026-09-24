import { useOutletContext } from "react-router-dom";
import type { DashboardContext } from "../ServerDashboard";

export function useOnlinePlayers() {
  return useOutletContext<DashboardContext>().onlinePlayers;
}
