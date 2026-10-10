import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { InputItem } from "../../../components/InputItem";
import { Portal } from "../../../components/Portal";
import { SelectItem } from "../../../components/SelectItem";
import { SubmitButton } from "../../../components/SubmitButton";
import { useToast } from "../../../context/ToastContext";
import { ramSizes, type Option } from "../../../data";
import { useCreateServer } from "../hooks/useCreateServer";
import { useServerVersions } from "../hooks/useServerVersions";

interface CreateServerProps {
  isOpen: boolean;
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>;
  onCreate: () => void;
}

const parseRam = (ram: string): number => parseInt(ram.replace("G", ""), 10);

export default function CreateServer({ isOpen, setIsOpen, onCreate }: CreateServerProps) {
  const { data } = useServerVersions();
  const { submitting, createServer } = useCreateServer();
  const { success, error } = useToast();

  const serverVersions: Option[] = useMemo(
    () => data.map((version) => ({ value: version.id, label: version.id })),
    [data],
  );

  const [serverVersion, setServerVersion] = useState<Option>(serverVersions[0]);
  const [serverName, setServerName] = useState<string>("");
  const [minRam, setMinRam] = useState<Option | null>(ramSizes[0]);
  const [maxRam, setMaxRam] = useState<Option | null>(ramSizes[1]);

  useEffect(() => {
    if (serverVersions.length > 0 && serverVersion === undefined) {
      setServerVersion(serverVersions[0]);
    }
  }, [serverVersions, serverVersion]);

  const navigate = useNavigate();

  const validate = (): boolean => {
    if (!serverName.trim()) {
      return false;
    }

    if (!serverVersion) {
      return false;
    }

    if (minRam && maxRam && parseRam(minRam.value) > parseRam(maxRam.value)) {
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (submitting || !validate()) return;

    const serverId = await createServer({
      name: serverName.trim(),
      version: serverVersion!.value,
      minRam: minRam!.value,
      maxRam: maxRam!.value,
    });

    setIsOpen(false);

    if (serverId) {
      success("Servidor criado com sucesso", {
        id: "create-server-success",
        duration: 2000,
      });
    } else {
      error("Erro ao criar servidor", {
        id: "create-server-error",
        duration: 2000,
      });
    }

    await onCreate();
    navigate(`/servers/${serverId}`);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") setIsOpen(false);
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
            onKeyDown={handleKeyDown}
          >
            <motion.div
              initial={{ y: -500, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: -500, opacity: 0 }}
              transition={{
                duration: 0.3,
              }}
              className="create-server-container"
            >
              <div className="create-server-header">
                <h2>Create New Server</h2>
              </div>
              <form className="create-server-form" onSubmit={handleSubmit}>
                <InputItem title="Server Name:" type="text" placeholder="My New Server" onChange={setServerName} />
                <SelectItem
                  title="Server Version:"
                  value={serverVersion}
                  options={serverVersions}
                  onChange={(v) => v && setServerVersion(v)}
                />
                <div className="create-server-quick-settings-container">
                  <h3>Quick settings</h3>
                  <div className="create-server-quick-settings">
                    <SelectItem
                      title="Minimum Memory:"
                      defaultValue={ramSizes[0]}
                      options={ramSizes}
                      onChange={setMinRam}
                    />
                    <SelectItem
                      title="Maximum Memory:"
                      defaultValue={ramSizes[1]}
                      options={ramSizes}
                      onChange={setMaxRam}
                    />
                  </div>
                </div>
                <SubmitButton type="submit" label="Create Server" loading={submitting} />
              </form>
            </motion.div>
            <X className="create-server-close-btn" onClick={() => setIsOpen(false)} />
          </motion.div>
        </Portal>
      )}
    </AnimatePresence>
  );
}
