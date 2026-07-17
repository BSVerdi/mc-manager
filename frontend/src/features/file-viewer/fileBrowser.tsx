import {
  File,
  FileCog,
  FileTerminal,
  FileText,
  Folder,
  MoveLeft,
  Undo2,
} from "lucide-react";
import { useFileBrowser } from "../../hooks/useFileBrowser";
import { useFileEditor } from "../../hooks/useFileEditor";
import type { Props } from ".";

function formatSize(bytes: number): string {
  if (bytes < 1024) return "--";
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function FileBrowser({ browser, editor }: Props) {
  const getFileIcon = (extension: string): React.ReactNode => {
    switch (extension) {
      case "":
        return (
          <Folder
            style={{ width: 40, height: 40, color: "#3b82f6", fill: "#eff6ff" }}
          />
        );
      case "txt":
        return <FileText style={{ width: 40, height: 40, color: "#6b7280" }} />;
      case "properties":
        return <FileCog style={{ width: 40, height: 40, color: "#6b7280" }} />;
      case "bat":
        return (
          <FileTerminal style={{ width: 40, height: 40, color: "#6b7280" }} />
        );
      case "jar":
        return (
          <FileTerminal style={{ width: 40, height: 40, color: "#6b7280" }} />
        );
      default:
        return <File style={{ width: 40, height: 40, color: "#9ca3af" }} />;
    }
  };

  const breadcrumbs = browser.currentPath.split("/").filter(Boolean);

  const handleClick = (name: string, directory: boolean) => {
    if (directory) {
      browser.openFolder(name);
    } else {
      const path = browser.currentPath
        ? `${browser.currentPath}/${name}`
        : name;
      editor.open(path);
    }
  };

  return (
    <>
      <section className="file-grid-container">
        <section className="breadcrumbs">
          <a onClick={() => browser.goToBreadcrumb(-1)}>root</a>
          {breadcrumbs.map((part, i) => (
            <span key={i}>
              <span className="breadcrumb-sep">/</span>
              <a
                className="breadcrumb-item"
                onClick={() => browser.goToBreadcrumb(i)}
              >
                {part}
              </a>
            </span>
          ))}
        </section>
        <section className="file-grid">
          {browser.currentPath && (
            <div className="file-card" onClick={browser.goUp}>
              <MoveLeft />
              <h3>..</h3>
            </div>
          )}
          {browser.entries.map((file) => (
            <div
              className="file-card"
              key={file.name}
              onClick={() => handleClick(file.name, file.directory)}
            >
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
          ))}
        </section>
      </section>
    </>
  );
}
