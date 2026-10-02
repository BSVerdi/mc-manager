import { useState } from "react";
import { Virtuoso } from "react-virtuoso";
import ConsoleLine from "./components/ConsoleLine";
import { useServerId } from "../hooks/useServerId";
import { useConsoleSocket } from "./hooks/useConsoleSocket";

export default function Console() {
  const serverId = useServerId();
  const { logs, sendCommand } = useConsoleSocket(Number(serverId));
  const [command, setCommand] = useState("");

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key !== "Enter") return;

    const trimmed = command.trim();
    if (trimmed === "") return;

    sendCommand(trimmed);
    setCommand("");
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
