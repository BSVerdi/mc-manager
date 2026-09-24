export type Metrics = {
  cpuUsagePercent: number;
  ramUsedMb: number;
  ramMaxMb: number;
  uptimeSeconds: number;
  onlinePlayers: number;
};

export const EMPTY_METRICS: Metrics = {
  cpuUsagePercent: 0,
  ramUsedMb: 0,
  ramMaxMb: 0,
  uptimeSeconds: 0,
  onlinePlayers: 0,
};
