import { Trash2, TriangleAlert } from "lucide-react";
import { useState } from "react";
import InputItem from "../components/InputItem";
import SelectItem from "../components/SelectItem";
import { ramSizes, type Option } from "../data/server";
import { useServerId } from "../hooks/useServerId";
import { api } from "../lib/apiClient";

export default function ServerSettings() {
  const serverId = useServerId();
  const [serverName, setServerName] = useState<string>("");
  const [minRam, setMinRam] = useState<Option | null>(ramSizes[0]);
  const [maxRam, setMaxRam] = useState<Option | null>(ramSizes[1]);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [deleting, setDeleting] = useState<boolean>(false);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (submitting) return;

    setSubmitting(true);

    await api.servers
      .update(serverId, {
        name: serverName,
        minRam: minRam?.value,
        maxRam: maxRam?.value,
      })
      .catch((err) => {
        console.error("Erro ao atualizar os dados do servidor: ", err);
        alert("Erro ao atualizar os dados do servidor.");
      })
      .finally(() => {
        setSubmitting(false);
      });
  };

  const handleDelete = async () => {
    if (deleting) return;

    setDeleting(true);

    await api.servers
      .delete(serverId)
      .catch((err) => {
        console.error("Erro ao deletar servidor.", err);
        alert("Erro ao deletar servidor");
      })
      .finally(() => {
        setDeleting(false);
        window.location.href = "/";
      });
  };

  return (
    <div className="server-settings-container">
      <div className="server-settings-form-container">
        <h3 className="server-settings-form-title">Server options</h3>
        <form className="server-settings-form" onSubmit={handleSubmit}>
          <InputItem
            title="Server Name"
            type="text"
            placeHolder="My New Server Name"
            setFunction={setServerName}
          />
          <SelectItem
            title="Minimum Memory"
            defaultValue={ramSizes[0]}
            options={ramSizes}
            setFunction={setMinRam}
          />
          <SelectItem
            title="Maximum Memory"
            defaultValue={ramSizes[1]}
            options={ramSizes}
            setFunction={setMaxRam}
          />
          <button className="form-submit-btn" type="submit">
            Update Server Data
          </button>
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
