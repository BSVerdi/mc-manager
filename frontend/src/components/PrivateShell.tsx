import { ServersProvider } from "../context/ServersContext";
import { MainLayout } from "../layouts/MainLayout";

export function PrivateShell() {
  return (
    <ServersProvider>
      <MainLayout />
    </ServersProvider>
  );
}
