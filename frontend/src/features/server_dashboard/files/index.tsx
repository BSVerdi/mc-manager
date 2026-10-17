import { useServerId } from "../hooks/useServerId";
import { FileBrowser } from "./components/FileBrowser";
import { FileEditor } from "./components/FileEditor";
import { useFileEditor } from "./hooks/useFileEditor";

export function FileExplorer() {
  const serverId = useServerId();
  const editor = useFileEditor(serverId);

  return (
    <>
      <section className="file-container">
        <FileBrowser serverId={serverId} openFile={editor.open} />

        {editor.file && <FileEditor editor={editor} />}
      </section>
    </>
  );
}
