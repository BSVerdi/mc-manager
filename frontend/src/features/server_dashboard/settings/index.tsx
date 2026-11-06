import { Trash2, TriangleAlert } from "lucide-react";
import { useEffect, useState } from "react";
import { InputItem } from "../../../components/InputItem";
import { SelectItem } from "../../../components/SelectItem";
import { SubmitButton } from "../../../components/SubmitButton";
import { useConfirmDialog } from "../../../context/ConfirmDialogContext";
import { ramSizes } from "../../../data";
import { useServerCatalog } from "../../../hooks/useServerCatalog";
import { EMPTY_OPTION, type Option } from "../../../types/option";
import { useServerId } from "../hooks/useServerId";
import { useServerSettings } from "./hooks/useServerSettings";

export function ServerSettings() {
  const [serverName, setServerName] = useState<string>("");
  const [minRam, setMinRam] = useState<Option>(ramSizes[0]);
  const [maxRam, setMaxRam] = useState<Option>(ramSizes[1]);
  const [serverType, setServerType] = useState<Option>(EMPTY_OPTION);
  const [serverVersion, setServerVersion] = useState<Option>(EMPTY_OPTION);

  const serverId = useServerId();
  const { submitting, updateServer, deleteServer } = useServerSettings(serverId);
  const { confirm } = useConfirmDialog();
  const { catalog, loading } = useServerCatalog();

  const providers: Option[] = Array.from(catalog.keys());
  const serverVersions = catalog.get(serverType) ?? [];

  useEffect(() => {
    if (loading || catalog.size === 0) return;

    const first = Array.from(catalog.keys())[0];
    setServerType(first);
    setServerVersion(catalog.get(first)![0]);
  }, [loading, catalog]);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (submitting) return;

    await updateServer({
      name: serverName,
      minRam: minRam?.value,
      maxRam: maxRam?.value,
      type: serverType?.value,
      version: serverVersion?.value,
    });
  };

  const handleDelete = async () => {
    if (submitting) return;

    const confirmed = await confirm({
      title: "Deletar servidor",
      description: "Deseja realmente deletar o servidor?",
      cancelLabel: "Cancelar",
      confirmLabel: "Deletar",
    });

    if (!confirmed) return;

    await deleteServer();
  };

  return (
    <>
      <div className="server-settings-container">
        <div className="server-settings-form-container">
          <h3 className="server-settings-form-title">Server options</h3>
          <form className="server-settings-form" onSubmit={handleSubmit}>
            <InputItem title="Server Name" type="text" placeholder="My New Server Name" onChange={setServerName} />
            <SelectItem
              title="Server type"
              value={serverType}
              options={providers}
              onChange={(t) => t && setServerType(t)}
            />
            <SelectItem
              title="Server Version:"
              value={serverVersion}
              options={serverVersions}
              onChange={(v) => v && setServerVersion(v)}
            />

            <SelectItem title="Minimum Memory" defaultValue={ramSizes[0]} options={ramSizes} onChange={setMinRam} />
            <SelectItem title="Maximum Memory" defaultValue={ramSizes[1]} options={ramSizes} onChange={setMaxRam} />
            <div className="server-settings-submit-container">
              <SubmitButton type="submit" label="Update Server Data" loading={submitting} disabled={submitting} />
            </div>
          </form>
        </div>

        <div className="server-settings-danger-actions-container">
          <h3 className="server-settings-danger-actions-title">
            <TriangleAlert />
            Danger Area
          </h3>
          <button className="delete-server-btn" onClick={handleDelete}>
            <Trash2 /> Delete Server
          </button>
        </div>
      </div>
    </>
  );
}
