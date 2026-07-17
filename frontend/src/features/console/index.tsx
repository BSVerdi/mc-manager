import { Virtuoso } from "react-virtuoso";
import ConsoleLine from "./ConsoleLine";
import { useConsoleSocket } from "../../hooks/useConsoleSocket";
import { useState } from "react";

export default function Console() {
  const { logs, sendCommand } = useConsoleSocket();
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
