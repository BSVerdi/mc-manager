import { AnimatePresence, motion } from "framer-motion";
import { File, FolderInput, Upload } from "lucide-react";
import { useRef, useState } from "react";

type UploadButtonProps = {
  onFileSelected: (file: File) => void;
  onFolderSelected: (FileList: FileList) => void;
  disable?: boolean;
};

export function UploadButton({ onFileSelected, onFolderSelected, disable }: UploadButtonProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const uploadModeRef = useRef<"file" | "folder">("file");
  const [menuOpen, setMenuOpen] = useState<boolean>(false);

  const openFilePicker = (mode: "file" | "folder") => {
    const input = inputRef.current;

    if (!input) return;

    uploadModeRef.current = mode;

    if (mode === "folder") {
      input.setAttribute("webkitdirectory", "");
      input.setAttribute("directory", "");
      input.multiple = true;
    } else {
      input.removeAttribute("webkitdirectory");
      input.removeAttribute("directory");
      input.multiple = false;
    }

    setMenuOpen(false);
    input.click();
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { files } = e.target;

    if (!files || files.length === 0) return;

    if (uploadModeRef.current === "folder") {
      onFolderSelected(files);
    } else {
      onFileSelected(files[0]);
    }

    e.target.value = "";
  };

  return (
    <div className="upload-btn-wrapper" ref={wrapperRef}>
      <button className="upload-btn" onClick={() => setMenuOpen((o) => !o)} disabled={disable} title="Enviar">
        <Upload style={{ color: "#9ca3af" }} />
      </button>

      <AnimatePresence>
        {menuOpen && (
          <motion.div
            initial={{
              opacity: 0,
              x: 20,
              y: 0,
              scale: 0.9,
              transition: { duration: 0.2 },
            }}
            animate={{
              opacity: 1,
              x: 0,
              y: 10,
              scale: 1,
              transition: {
                type: "spring",
                stiffness: 300,
                mass: 0.8,
              },
            }}
            exit={{
              opacity: 0,
              x: 20,
              y: 0,
              scale: 0.9,
              transition: { duration: 0.2 },
            }}
            className="upload-menu"
          >
            <button className="upload-menu-item" onClick={() => openFilePicker("file")}>
              <File size={16} /> Enviar arquivo
            </button>
            <button className="upload-menu-item" onClick={() => openFilePicker("folder")}>
              <FolderInput size={16} /> Enviar pasta
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      <input ref={inputRef} type="file" hidden onChange={handleChange} />
    </div>
  );
}
