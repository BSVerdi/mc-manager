import { useEffect } from "react";
import SideMenu from "../features/navigation/SideMenu";
import { Outlet } from "react-router-dom";
import { onStompAuthError } from "../lib/stompClient";

export default function MainLayout() {
  useEffect(() => {
    return onStompAuthError((message) => {
      alert(`Acesso negado: ${message}`);
    });
  }, []);

  return (
    <div className="app-container">
      <SideMenu />
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
