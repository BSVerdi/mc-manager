import { useOutletContext } from "react-router-dom";
import type { DashboardContext } from "../..";

export function useOnlinePlayers() {
  return useOutletContext<DashboardContext>().onlinePlayers;
}
