import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { PrivateRoute } from "./components/PrivateRoute";
import { PublicOnlyRoute } from "./components/PublicOnlyRoute";
import { AuthProvider } from "./context/AuthContext";
import { LoadingProvider } from "./context/LoadingContext";
import AuthLayout from "./layouts/AuthLayout";
import MainLayout from "./layouts/MainLayout";
import Console from "./pages/Console";
import FileViewer from "./pages/FileViewer";
import Home from "./pages/Home";
import Login from "./pages/Login";
import PlayersList from "./pages/PlayerList";
import Register from "./pages/Register";
import ServerDashboard from "./pages/ServerDashboard";
import ServerSettings from "./pages/ServerSettings";
import Settings from "./pages/Settings";

export default function Router() {
  return (
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
              <Route path="/" element={<MainLayout />}>
                <Route index element={<Home />} />
                <Route path="servers/:serverId" element={<ServerDashboard />}>
                  <Route index element={<Navigate to="console" replace />} />
                  <Route path="console" element={<Console />} />
                  <Route path="files" element={<FileViewer />} />
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
  );
}
