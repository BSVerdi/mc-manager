import { Download, File, FileCog, FileTerminal, FileText, Folder, ImageIcon, MoveLeft, Trash2 } from "lucide-react";
import { useState } from "react";
import LoadingScreen from "../../../components/LoadingScreen";
import UploadButton from "../../../components/UploadButton";
import { zipFolder } from "../../../lib/zipFolder";
import type { FileExplorerProps } from "./index";

function formatSize(bytes: number): string {
  if (bytes < 1024) return "--";
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function FileBrowser({ browser, editor }: FileExplorerProps) {
  const [deleting, setDeleting] = useState<boolean>(false);
  const [zipping, setZipping] = useState<boolean>(false);

  const getFileIcon = (extension: string): React.ReactNode => {
    switch (extension) {
      case "":
        return <Folder style={{ width: 40, height: 40, color: "#3b82f6", fill: "#eff6ff" }} />;
      case "txt":
        return <FileText style={{ width: 40, height: 40, color: "#6b7280" }} />;
      case "properties":
        return <FileCog style={{ width: 40, height: 40, color: "#6b7280" }} />;
      case "bat":
        return <FileTerminal style={{ width: 40, height: 40, color: "#6b7280" }} />;
      case "jar":
        return <FileTerminal style={{ width: 40, height: 40, color: "#6b7280" }} />;
      case "png":
        return <ImageIcon style={{ width: 40, height: 40, color: "#a855f7" }} />;
      default:
        return <File style={{ width: 40, height: 40, color: "#9ca3af" }} />;
    }
  };

  const breadcrumbs = browser.currentPath.split("/").filter(Boolean);

  const handleClick = (name: string, directory: boolean) => {
    if (directory) {
      browser.openFolder(name);
    } else {
      const path = browser.currentPath ? `${browser.currentPath}/${name}` : name;
      editor.open(path);
    }
  };

  const handleDelete = async (path: string) => {
    if (deleting) return;

    setDeleting(true);
    await browser.remove(path);
    setDeleting(false);
  };

  return (
    <section className="file-grid-container">
      <LoadingScreen isVisible={zipping} />
      <section className="file-grid-header">
        <div>
          <a onClick={() => browser.goToBreadcrumb(-1)}>root</a>
          {breadcrumbs.map((part, i) => (
            <span key={i}>
              <span className="breadcrumb-sep">/</span>
              <a className="breadcrumb-item" onClick={() => browser.goToBreadcrumb(i)}>
                {part}
              </a>
            </span>
          ))}
        </div>
        <UploadButton
          disable={zipping}
          onFileSelected={(file) => browser.upload(file)}
          onFolderSelected={async (fileList) => {
            setZipping(true);

            try {
              const zipFile = await zipFolder(fileList);
              await browser.uploadZip(zipFile);
            } catch (err) {
              console.error("Erro ao compactar pasta: ", err);
              alert("Erro ao compactar pasta");
            } finally {
              setZipping(false);
            }
          }}
        />
      </section>
      <section className="file-grid">
        {browser.currentPath && (
          <div className="file-card walk-back" onClick={browser.goUp}>
            <MoveLeft />
            <h3>..</h3>
          </div>
        )}
        {browser.entries.map((file) => {
          const entryPath = browser.currentPath ? `${browser.currentPath}/${file.name}` : file.name;

          const downloadHref = browser.getDownloadUrl(entryPath, file.directory);

          return (
            <div className="file-card" key={file.name}>
              <div className="file-item-main" onClick={() => handleClick(file.name, file.directory)}>
                <div className="file-icon">{getFileIcon(file.extension)}</div>
                <div className="file-details">
                  <h3 className="file-title">{file.name}</h3>
                  <div className="file-meta">
                    <span>
                      Última modificação:
                      {new Date(file.lastModified).toLocaleDateString()}
                    </span>
                    {formatSize(file.sizeBytes) !== "--" && (
                      <>
                        <span>•</span>
                        <span>{formatSize(file.sizeBytes)}</span>
                      </>
                    )}
                  </div>
                </div>
              </div>
              <div className="file-btns-container">
                <a className="download-btn" href={downloadHref}>
                  <Download
                    style={{
                      width: 24,
                      height: 24,
                      color: "#9ca3af",
                    }}
                  />
                </a>
                <Trash2
                  style={{
                    width: 24,
                    height: 24,
                    color: "#ff3535",
                  }}
                  className="file-delete-btn"
                  onClick={() => handleDelete(entryPath)}
                />
              </div>
            </div>
          );
        })}
      </section>
    </section>
  );
}
