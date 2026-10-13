import { ConfirmDialogProvider } from "../context/ConfirmDialogContext";
import { ServersProvider } from "../context/ServersContext";
import { MainLayout } from "../layouts/MainLayout";

export function PrivateShell() {
  return (
    <ConfirmDialogProvider>
      <ServersProvider>
        <MainLayout />
      </ServersProvider>
    </ConfirmDialogProvider>
  );
}
