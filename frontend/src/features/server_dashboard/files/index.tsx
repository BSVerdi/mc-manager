import { useServerId } from "../hooks/useServerId";
import { FileBrowser } from "./components/FileBrowser";
import { FileEditor } from "./components/FileEditor";
import { useFileBrowser, type FilesBrowserApi } from "./hooks/useFileBrowser";
import { useFileEditor, type FileEditorApi } from "./hooks/useFileEditor";

export interface FileExplorerProps {
  browser: FilesBrowserApi;
  editor: FileEditorApi;
}

export function FileExplorer() {
  const serverId = useServerId();
  const browser = useFileBrowser(serverId);
  const editor = useFileEditor(serverId);

  return (
    <>
      <section className="file-container">
        <FileBrowser browser={browser} editor={editor} />

        {editor.file && <FileEditor browser={browser} editor={editor} />}
      </section>
    </>
  );
}
