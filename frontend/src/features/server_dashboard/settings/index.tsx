import { Trash2, TriangleAlert } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import InputItem from "../../../components/InputItem";
import SelectItem from "../../../components/SelectItem";
import SubmitButton from "../../../components/SubmitButton";
import { ramSizes, type Option } from "../../../data/server";
import { useServerId } from "../useServerId";
import { useServerSettings } from "./useServerSettings";

export default function ServerSettings() {
  const [serverName, setServerName] = useState<string>("");
  const [minRam, setMinRam] = useState<Option | null>(ramSizes[0]);
  const [maxRam, setMaxRam] = useState<Option | null>(ramSizes[1]);

  const serverId = useServerId();
  const navigate = useNavigate();
  const { submitting, updateServer, deleteServer } = useServerSettings(serverId);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (submitting) return;

    await updateServer({
      name: serverName,
      minRam: minRam?.value,
      maxRam: maxRam?.value,
    });
  };

  const handleDelete = async () => {
    if (submitting) return;

    await deleteServer();
    navigate("/");
  };

  return (
    <div className="server-settings-container">
      <div className="server-settings-form-container">
        <h3 className="server-settings-form-title">Server options</h3>
        <form className="server-settings-form" onSubmit={handleSubmit}>
          <InputItem title="Server Name" type="text" placeholder="My New Server Name" onChange={setServerName} />
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
  );
}
