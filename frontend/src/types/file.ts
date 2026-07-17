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
