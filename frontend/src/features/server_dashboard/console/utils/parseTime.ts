export function parseTime(rawTime: string) {
  const date = new Date(rawTime);
  const time = Number.isFinite(date.getTime()) ? date.toLocaleTimeString("pt-BR", { hour12: false }) : "--";

  return time;
}
