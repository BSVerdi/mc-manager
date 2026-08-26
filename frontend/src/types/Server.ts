export type Server = {
  id: number;
  name: string;
  port: number;
  minRam: string;
  maxRam: string;
  version: string;
};

export type CreateServerRequest = {
  name: string;
  version: string;
  port: number;
  minRam?: string;
  maxRam?: string;
};

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
