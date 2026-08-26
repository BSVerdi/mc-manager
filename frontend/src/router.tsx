import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import { AuthProvider } from "./context/AuthContext";
import { PublicOnlyRoute } from "./components/PublicOnlyRoute";
import { PrivateRoute } from "./components/PrivateRoute";
import MainLayout from "./layouts/MainLayout";
import Home from "./pages/Home";
import ServerDashboard from "./pages/ServerDashboard";
import Console from "./pages/Console";
import FileViewer from "./pages/FileViewer";
import PlayersList from "./pages/PlayerList";
import AuthLayout from "./layouts/AuthLayout";
import CreateServer from "./features/create_server/CreateServer";
import Settings from "./pages/Settings";
import ServerSettings from "./pages/ServerSettings";

export default function Router() {
  return (
    <AuthProvider>
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
    </AuthProvider>
  );
}
