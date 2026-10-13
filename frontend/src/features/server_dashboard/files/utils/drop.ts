import { readDirectory, type UploadEntry } from "./zipFolder";

type Payload = { kind: "single"; entry: UploadEntry } | { kind: "bundle"; entries: UploadEntry[]; zipName: string };

export async function collectFromDrop(dt: DataTransfer): Promise<Payload | null> {
  const loose: UploadEntry[] = [];
  let folder: FileSystemDirectoryEntry | null = null;

  for (const item of Array.from(dt.items)) {
    if (item.kind !== "file") continue;

    const entry = item.webkitGetAsEntry?.();

    if (!entry) {
      const file = item.getAsFile();

      if (file) loose.push({ file, path: file.name });
      continue;
    }

    if (entry.isFile) {
      const file = await new Promise<File>((resolve, reject) => (entry as FileSystemFileEntry).file(resolve, reject));
      loose.push({ file, path: file.name });
    } else {
      folder = entry as FileSystemDirectoryEntry;
    }
  }

  if (folder) {
    const entries = await readDirectory(folder, `${folder.name}/`);

    return { kind: "bundle", entries, zipName: `${folder.name}.zip` };
  }

  if (loose.length === 1) {
    return { kind: "single", entry: loose[0] };
  }

  if (loose.length > 1) {
    return { kind: "bundle", entries: loose, zipName: "upload.zip" };
  }

  return null;
}
