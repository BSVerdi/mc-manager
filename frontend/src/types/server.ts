export type Server = {
  id: number;
  name: string;
  port: number;
  minRam: string;
  maxRam: string;
  version: string;
};

export type ServerVersion = {
  id: string;
  type: string;
  url: string;
};
