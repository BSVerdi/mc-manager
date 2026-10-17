import type { UploadEntry } from "./utils/zipFolder";

export type FileEntry = {
  name: string;
  directory: boolean;
  sizeBytes: number;
  lastModified: number;
  extension: string;
};

export type FileContent = {
  path: string;
  content: string;
  editable: boolean;
  extension: string;
};

export type Payload =
  | { kind: "single"; entry: UploadEntry }
  | { kind: "bundle"; entries: UploadEntry[]; zipName: string };

export type UploadState = "zipping" | "uploading" | null;
