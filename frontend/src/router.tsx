import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { PrivateRoute } from "./components/PrivateRoute";
import { PrivateShell } from "./components/PrivateShell";
import { PublicOnlyRoute } from "./components/PublicOnlyRoute";
import { AuthProvider } from "./context/AuthContext";
import { LoadingProvider } from "./context/LoadingContext";
import { ToastProvider } from "./context/ToastContext";
import { Login } from "./features/auth/Login";
import { Register } from "./features/auth/Register";
import { Home } from "./features/home";
import { ServerDashboard } from "./features/server_dashboard";
import { Console } from "./features/server_dashboard/console";
import { FileExplorer } from "./features/server_dashboard/files";
import { PlayersList } from "./features/server_dashboard/players";
import { ServerSettings } from "./features/server_dashboard/settings";
import { Settings } from "./features/settings";
import { AuthLayout } from "./layouts/AuthLayout";

export default function Router() {
  return (
    <ToastProvider>
      <AuthProvider>
        <LoadingProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<PublicOnlyRoute />}>
                <Route path="/" element={<AuthLayout />}>
                  <Route path="login" element={<Login />} />
                  <Route path="register" element={<Register />} />
                </Route>
              </Route>
              <Route element={<PrivateRoute />}>
                <Route path="/" element={<PrivateShell />}>
                  <Route index element={<Home />} />
                  <Route path="servers/:serverId" element={<ServerDashboard />}>
                    <Route index element={<Navigate to="console" replace />} />
                    <Route path="console" element={<Console />} />
                    <Route path="files" element={<FileExplorer />} />
                    <Route path="players" element={<PlayersList />} />
                    <Route path="settings" element={<ServerSettings />} />
                  </Route>
                  <Route path="settings" element={<Settings />} />
                </Route>
              </Route>
            </Routes>
          </BrowserRouter>
        </LoadingProvider>
      </AuthProvider>
    </ToastProvider>
  );
}
