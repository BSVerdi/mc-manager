import { useFileBrowser } from "../../hooks/useFileBrowser";
import { useFileEditor } from "../../hooks/useFileEditor";
import { FileBrowser } from "./fileBrowser";
import { FileEditor } from "./fileEditor";

export type Props = {
  browser: ReturnType<typeof useFileBrowser>;
  editor: ReturnType<typeof useFileEditor>;
};

export default function FileViewer() {
  const browser = useFileBrowser();
  const editor = useFileEditor();

  return (
    <>
      <section className="file-container">
        <FileBrowser browser={browser} editor={editor} />

        {editor.file && <FileEditor browser={browser} editor={editor} />}
      </section>
    </>
  );
}
