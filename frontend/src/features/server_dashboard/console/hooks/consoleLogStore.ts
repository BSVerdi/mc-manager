import type { Log } from "../types";

const MAX_LINES = 2000;
const EMPTY: Log[] = [];

const buffers = new Map<number, Log[]>();
const listeners = new Set<() => void>();

function emit() {
  for (const listener of listeners) listener();
}

export const consoleLogStore = {
  getLogs: (serverId: number): Log[] => buffers.get(serverId) ?? EMPTY,

  subscribe: (listener: () => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },

  push: (serverId: number, log: Log) => {
    const next = [...(buffers.get(serverId) ?? []), log];
    buffers.set(serverId, next.length > MAX_LINES ? next.slice(-MAX_LINES) : next);
    emit();
  },

  clear: (serverId: number) => {
    buffers.set(serverId, EMPTY);
    emit();
  },

  remove: (serverId: number) => {
    buffers.delete(serverId);
    emit();
  },
};
