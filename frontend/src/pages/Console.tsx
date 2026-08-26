import { Virtuoso } from "react-virtuoso";
import ConsoleLine from "../components/ConsoleLine";
import { useConsoleSocket } from "../hooks/useConsoleSocket";
import { useState } from "react";
import { useParams } from "react-router-dom";
import { useServerId } from "../hooks/useServerId";

export default function Console() {
  const serverId = useServerId();
  const { logs, sendCommand } = useConsoleSocket(Number(serverId));
  const [command, setCommand] = useState("");

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && command.trim() !== "") {
      sendCommand(command.trim());
      setCommand("");
    }
  };

  return (
    <>
      <section className="console">
        <Virtuoso
          className="terminal"
          data={logs}
          followOutput="smooth"
          itemContent={(_, log) => <ConsoleLine log={log} />}
        />
        <input
          type="text"
          className="commandInput"
          placeholder="Insira o comando"
          value={command}
          onChange={(e) => setCommand(e.target.value)}
          onKeyDown={handleKeyDown}
        />
      </section>
    </>
  );
}
