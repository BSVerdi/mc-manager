import { createPortal } from "react-dom";

interface PortalProps {
  children: React.ReactNode;
}

export function Portal({ children }: PortalProps) {
  const mountNode = document.getElementById("portal-root");

  if (!mountNode) return null;

  return createPortal(children, mountNode);
}
