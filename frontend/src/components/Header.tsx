import { AnimatePresence, motion } from "framer-motion";
import { ChevronsLeft, LogOut, Settings } from "lucide-react";
import { useState, type Dispatch, type SetStateAction } from "react";
import { useNavigate } from "react-router-dom";
import defaultProfilePic from "../assets/big-steve-face.png";
import { api } from "../lib/apiClient";
import type { User } from "../types/user";

interface HeaderProps {
  opened: boolean;
  setOpened: Dispatch<SetStateAction<boolean>>;
  user?: User | null;
}

export default function Header({ opened, setOpened, user }: HeaderProps) {
  const [userSettings, setUserSettings] = useState<boolean>(false);
  const [loggingOut, setLogginOut] = useState<boolean>(false);
  const navigate = useNavigate();

  const toggleSideMenu = () => setOpened((prev) => !prev);

  const toggleUserSettings = () => setUserSettings((prev) => !prev);

  const handleLogout = async () => {
    if (loggingOut) return;

    setLogginOut(true);

    api.auth
      .logout()
      .catch((error) => {
        console.error("Erro ao realizar logout: ", error);
        alert("Erro ao realizar logout");
      })
      .then(() => window.location.reload())
      .finally(() => setLogginOut(false));
  };

  return (
    <div className="header">
      <motion.div
        animate={{ rotate: opened ? 0 : 180 }}
        transition={{ duration: 0.1 }}
        className="side-menu-state-container"
      >
        <ChevronsLeft
          className="side-menu-state-btn"
          onClick={toggleSideMenu}
        />
      </motion.div>
      <motion.div
        className="user-container"
        initial={false}
        animate={{ width: userSettings ? "150px" : "60px" }}
        transition={{ duration: 0.15 }}
      >
        <motion.button
          className="user-pfp-container"
          onClick={toggleUserSettings}
        >
          <img
            src={
              user?.profilePictureUrl
                ? user?.profilePictureUrl
                : defaultProfilePic
            }
            className="user-pfp"
          ></img>
        </motion.button>
        <AnimatePresence>
          {userSettings && (
            <motion.div
              layout
              className="user-btns"
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.125 }}
            >
              <Settings
                className="user-btn"
                style={{ width: 32, right: 32, color: "#6b7280" }}
                onClick={() => navigate("/settings")}
              />
              <LogOut
                className="user-btn"
                style={{ width: 32, right: 32, color: "red" }}
                onClick={handleLogout}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
}
