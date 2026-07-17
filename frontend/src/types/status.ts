export type Status =
  | "OFFLINE"
  | "STARTING"
  | "ONLINE"
  | "STOPPING"
  | "RESTARTING";
export const defaultStatus: Status = "OFFLINE";
