import { useConfirmDialog } from "../../../context/ConfirmDialogContext";
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
  const { confirm } = useConfirmDialog();
  const browser = useFileBrowser(serverId);
  const editor = useFileEditor(serverId, confirm);

  return (
    <>
      <section className="file-container">
        <FileBrowser browser={browser} openFile={editor.open} />

        {editor.file && <FileEditor editor={editor} />}
      </section>
    </>
  );
}
