import Select from "react-select";
import type { Option } from "../data/server";

type SelectItemProps = {
  title: string;
  defaultValue: Option;
  options: Option[];
  setFunction: (value: any) => void;
};

export default function SelectItem({
  title,
  defaultValue,
  options,
  setFunction,
}: SelectItemProps) {
  return (
    <div className="select-item-container">
      <span className="select-item-title">{title}</span>
      <Select
        defaultValue={defaultValue}
        options={options}
        onChange={(value) => setFunction(value)}
        className="select-item"
        classNamePrefix="select"
      />
    </div>
  );
}
