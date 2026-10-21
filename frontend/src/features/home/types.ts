export type CreateServerRequest = {
  name: string;
  version: string;
  minRam?: string;
  maxRam?: string;
  type: string;
};
