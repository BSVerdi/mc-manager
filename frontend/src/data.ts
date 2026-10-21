export type Option = {
  value: string;
  label: string;
};

export const ramSizes: Option[] = [
  { value: "1G", label: "1GB" },
  { value: "2G", label: "2GB" },
  { value: "3G", label: "3GB" },
  { value: "4G", label: "4GB" },
  { value: "5G", label: "5GB" },
  { value: "6G", label: "6GB" },
];

export const serverTypes: Option[] = [
  { value: "VANILLA", label: "Vanilla" },
  { value: "PAPER", label: "Paper" },
];
