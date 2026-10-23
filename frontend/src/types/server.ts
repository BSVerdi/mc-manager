export type Server = {
  id: number;
  name: string;
  minRam: string;
  maxRam: string;
  version: string;
  domain: string;
};

export type Catalog = {
  provider: string;
  versions: string[];
};
