import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import InputItem from "../../components/InputItem";
import SelectItem from "../../components/SelectItem";
import { ramSizes, type Option } from "../../data/server";
import { useServerVersions } from "../../hooks/useServerVersions";
import { api } from "../../lib/apiClient";

export const Portal = ({ children }: { children: React.ReactNode }) => {
  const mountNode = document.getElementById("portal-root");

  if (!mountNode) return null;

  return createPortal(children, mountNode);
};

export type CreateServerProps = {
  isOpen: boolean;
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>;
};

export default function CreateServer({ isOpen, setIsOpen }: CreateServerProps) {
  const serverVersions: Option[] = useServerVersions().map((version) => ({
    value: version.id,
    label: version.id,
  }));

  const latestVersion = serverVersions[0];

  const [serverVersion, setServerVersion] = useState<Option | null>(
    latestVersion,
  );
  const [serverName, setServerName] = useState<string>("");
  const [minRam, setMinRam] = useState<Option | null>(ramSizes[0]);
  const [maxRam, setMaxRam] = useState<Option | null>(ramSizes[1]);
  const [serverPort, setSeverPort] = useState<number>(25565);
  const [submitting, setSubmitting] = useState<boolean>(false);

  const navigate = useNavigate();

  const handleSubmit = async () => {
    if (submitting) return;

    setSubmitting(true);
    api.servers
      .create({
        name: serverName,
        version: serverVersion!.value,
        port: serverPort,
        minRam: minRam?.value,
        maxRam: maxRam?.value,
      })
      .then((response) => navigate(`/servers/${response.id}/`))
      .catch((err) => {
        console.error("Erro ao criar servidor: ", err);
        alert("Erro ao criar servidor!");
      })
      .finally(() => {
        setSubmitting(false);
      });
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <Portal>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="create-server-overlay"
          >
            <motion.div
              initial={{ y: -500, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: -500, opacity: 0 }}
              transition={{
                type: "spring",
                stiffness: 100,
                damping: 18,
              }}
              className="create-server-container"
            >
              <div className="create-server-header">
                <h2>Create New Server</h2>
              </div>
              <form className="create-server-form">
                <InputItem
                  title="Server Name:"
                  type="text"
                  placeHolder="My New Server"
                  setFunction={setServerName}
                />
                <SelectItem
                  title="Server Version:"
                  defaultValue={latestVersion}
                  options={serverVersions}
                  setFunction={setServerVersion}
                />
                <div className="create-server-quick-settings-container">
                  <h3>Quick settings</h3>
                  <div className="create-server-quick-settings">
                    <SelectItem
                      title="Minimum Memory:"
                      defaultValue={ramSizes[0]}
                      options={ramSizes}
                      setFunction={setMinRam}
                    />
                    <SelectItem
                      title="Maximum Memory:"
                      defaultValue={ramSizes[1]}
                      options={ramSizes}
                      setFunction={setMaxRam}
                    />
                    <InputItem
                      title="Server Port"
                      type="number"
                      defaultValue="25565"
                      setFunction={setSeverPort}
                    />
                  </div>
                </div>
                <button
                  className="form-submit-btn"
                  onClick={handleSubmit}
                  disabled={submitting}
                >
                  Create Server
                </button>
              </form>
            </motion.div>
            <X
              className="create-server-close-btn"
              onClick={() => setIsOpen(false)}
            />
          </motion.div>
        </Portal>
      )}
    </AnimatePresence>
  );
}
