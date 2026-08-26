export enum Status {
  OFFLINE = "OFFLINE",
  STARTING = "STARTING",
  ONLINE = "ONLINE",
  STOPPING = "STOPPING",
  RESTARTING = "RESTARTING",
}

export const defaultStatus: Status = Status.OFFLINE;
