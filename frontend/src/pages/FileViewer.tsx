import { useFileBrowser } from "../hooks/useFileBrowser";
import { useFileEditor } from "../hooks/useFileEditor";
import { FileBrowser } from "../features/file-viewer/fileBrowser";
import { FileEditor } from "../features/file-viewer/fileEditor";
import { useServerId } from "../hooks/useServerId";

export type Props = {
  browser: ReturnType<typeof useFileBrowser>;
  editor: ReturnType<typeof useFileEditor>;
};

export default function FileViewer() {
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
