import { useServerId } from "../useServerId";
import { FileBrowser } from "./components/fileBrowser";
import { FileEditor } from "./components/fileEditor";
import { useFileBrowser, type FilesBrowserApi } from "./hooks/useFileBrowser";
import { useFileEditor, type FileEditorApi } from "./hooks/useFileEditor";

export interface FileExplorerProps {
  browser: FilesBrowserApi;
  editor: FileEditorApi;
}

export default function FileExplorer() {
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
