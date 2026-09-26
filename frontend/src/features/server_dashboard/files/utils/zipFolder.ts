import JSZip from "jszip";

export async function zipFolder(
  fileList: FileList,
  zipName = "upload.zip",
): Promise<File> {
  const zip = new JSZip();

  for (const file of Array.from(fileList)) {
    const relativePath =
      (file as File & { webkitRelativePath?: string }).webkitRelativePath ||
      file.name;

    zip.file(relativePath, file);
  }

  const blob = await zip.generateAsync({
    type: "blob",
    compression: "DEFLATE",
    compressionOptions: { level: 6 },
  });

  return new File([blob], zipName, { type: "application/zip" });
}
