export type UpdateServerRequest = {
  name: string;
  minRam?: string;
  maxRam?: string;
};

export type ServerVersion = {
  id: string;
  type: string;
  url: string;
};
