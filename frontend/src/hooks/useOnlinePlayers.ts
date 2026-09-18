import { useOutletContext } from "react-router-dom";
import type { DashboardContext } from "../pages/ServerDashboard";

export function useOnlinePlayers() {
  return useOutletContext<DashboardContext>().onlinePlayers;
}
