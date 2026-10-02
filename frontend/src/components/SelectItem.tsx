import { forwardRef, useId } from "react";
import Select from "react-select";
import type { Option } from "../data/data";

interface SelectItemProps {
  title: string;
  defaultValue?: Option;
  options: Option[];
  onChange: (value: Option) => void;
  placeholder?: string;
  isDisabled?: boolean;
  value?: Option;
}

const SelectItem = forwardRef<HTMLDivElement, SelectItemProps>(function SelectItem(
  { title, defaultValue, options, onChange, placeholder = "Selecione…", isDisabled = false, value },
  ref,
) {
  const id = useId();

  return (
    <div className="select-item-container" ref={ref}>
      <label className="select-item-title" htmlFor={id}>
        {title}
      </label>
      <Select
        id={id}
        value={value}
        defaultValue={defaultValue}
        options={options}
        placeholder={placeholder}
        isDisabled={isDisabled}
        isClearable={false}
        onChange={(selected) => {
          if (selected) onChange(selected);
        }}
        className="select-item"
        classNamePrefix="select"
      />
    </div>
  );
});

export default SelectItem;
