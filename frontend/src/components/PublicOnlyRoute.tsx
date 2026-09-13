import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import LoadingScreen from "./LoadingScreen";

export function PublicOnlyRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <LoadingScreen isVisible={loading} />;
  }

  if (user) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
