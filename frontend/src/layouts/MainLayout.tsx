import { useEffect } from "react";
import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import { useAuth } from "../context/AuthContext";
import { useLoading } from "../context/LoadingContext";
import SideMenu from "../features/navigation/SideMenu";
import { useSidebarState } from "../hooks/useSidebarState";
import { onStompAuthError } from "../lib/stompClient";

export default function MainLayout() {
  const [opened, setOpened] = useSidebarState();
  const { isLoading } = useLoading();
  const { user } = useAuth();

  useEffect(() => {
    return onStompAuthError((message) => {
      alert(`Acesso negado: ${message}`);
    });
  }, []);

  return (
    <div
      className="app-container"
      style={{ opacity: isLoading ? 0.5 : 1, transition: "opacity 0.3s" }}
    >
      <aside>
        <SideMenu opened={opened} />
      </aside>
      <div className="content-container">
        <header className="header-area">
          <Header opened={opened} setOpened={setOpened} user={user} />
        </header>
        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
