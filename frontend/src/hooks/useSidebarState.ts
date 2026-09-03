import { useEffect, useState } from "react";

const STORAGE_KEY = "sidebar-opened";

export function useSidebarState() {
  const [opened, setOpened] = useState<boolean>(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored !== null ? stored === "true" : true;
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, String(opened));
  }, [opened]);

  return [opened, setOpened] as const;
}
