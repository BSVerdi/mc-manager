import JSZip from "jszip";

export interface UploadEntry {
  file: File;
  path: string;
}

type WebKitFile = File & { webkitRelativePath?: string };

export async function readDirectory(dir: FileSystemDirectoryEntry, base = ""): Promise<UploadEntry[]> {
  const result: UploadEntry[] = [];
  const reader = dir.createReader();

  let batch: FileSystemEntry[] = await new Promise((resolve) => reader.readEntries(resolve));

  while (batch.length > 0) {
    for (const entry of batch) {
      if (entry.isFile) {
        const file = await new Promise<File>((resolve, reject) => (entry as FileSystemFileEntry).file(resolve, reject));
        result.push({ file, path: `${base}${entry.name}` });
      } else if (entry.isDirectory) {
        result.push(...(await readDirectory(entry as FileSystemDirectoryEntry, `${base}${entry.name}/`)));
      }
    }

    batch = await new Promise((resolve) => reader.readEntries(resolve));
  }

  return result;
}

export function zipEntries(entries: UploadEntry[], zipName: string): Promise<File> {
  const zip = new JSZip();

  for (const { file, path } of entries) {
    zip.file(path, file);
  }

  const seen = new Set<string>();
  for (const { path } of entries) {
    const parts = path.split("/");

    for (let i = 1; i < parts.length; i++) {
      const dir = parts.slice(0, i).join("/");
      if (!seen.has(dir)) {
        seen.add(dir);
        zip.folder(dir);
      }
    }
  }

  return zip
    .generateAsync({
      type: "blob",
      compression: "DEFLATE",
      compressionOptions: { level: 6 },
    })
    .then((blob) => new File([blob], zipName, { type: "application/zip" }));
}

export function fileListToEntries(fileList: FileList): UploadEntry[] {
  return Array.from(fileList).map((file) => ({
    file,
    path: (file as WebKitFile).webkitRelativePath || file.name,
  }));
}
