import type { LucideIcon } from "lucide-react";

export type MenuItem = {
  id: string;
  icon?: LucideIcon;
  path?: string;
  label: string;
  children?: MenuItem[];
};
