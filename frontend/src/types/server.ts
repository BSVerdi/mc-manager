export type Server = {
  id: number;
  name: string;
  minRam: string;
  maxRam: string;
  version: string;
  domain: string;
};

export type ServerVersion = {
  id: string;
  type: string;
  url: string;
};
