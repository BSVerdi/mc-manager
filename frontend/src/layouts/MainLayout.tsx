import { useCallback, useEffect } from "react";
import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import { useToast } from "../components/toast";
import { useAuth } from "../context/AuthContext";
import { useLoading } from "../context/LoadingContext";
import SideMenu from "../features/sideMenu";
import { useSidebarState } from "../hooks/useSidebarState";
import { onStompAuthError } from "../lib/stompClient";

export default function MainLayout() {
  const [opened, setOpened] = useSidebarState();
  const { isLoading } = useLoading();
  const { user } = useAuth();
  const { error } = useToast();

  const toggleSideBar = useCallback(() => setOpened(!opened), [opened, setOpened]);

  useEffect(() => {
    return onStompAuthError((message) => {
      error(`Acesso negado: ${message}`, {
        id: "stopm-auth-error",
        duration: 5000,
      });
    });
  }, []);

  return (
    <div
      className="app-container"
      style={{ opacity: isLoading ? 0.5 : 1, pointerEvents: isLoading ? "none" : "auto", transition: "opacity 0.3s" }}
    >
      <aside>
        <SideMenu opened={opened} />
      </aside>
      <div className="content-container">
        <header className="header-area">
          <Header opened={opened} onToggleSideBar={toggleSideBar} user={user} />
        </header>
        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
