import { useServerId } from "../useServerId";
import { FileBrowser } from "./fileBrowser";
import { FileEditor } from "./fileEditor";
import { useFileBrowser } from "./useFileBrowser";
import { useFileEditor } from "./useFileEditor";

export interface FileExplorerProps {
  browser: ReturnType<typeof useFileBrowser>;
  editor: ReturnType<typeof useFileEditor>;
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
