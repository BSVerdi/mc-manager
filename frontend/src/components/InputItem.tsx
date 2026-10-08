import { forwardRef, useId } from "react";

type InputType = "text" | "email" | "password" | "number" | "search" | "tel" | "url";

interface InputItemProps {
  type: InputType;
  title: string;
  placeholder?: string;
  defaultValue?: string;
  value?: string;
  onChange: (value: string) => void;
  required?: boolean;
  disabled?: boolean;
  name?: string;
}

export const InputItem = forwardRef<HTMLInputElement, InputItemProps>(function InputItem(
  { type, title, placeholder, defaultValue, value, onChange, required = false, disabled = false, name },
  ref,
) {
  const id = useId();

  return (
    <div className="input-item-container">
      <label className="input-item-title" htmlFor={id}>
        {title}
      </label>
      <input
        ref={ref}
        id={id}
        type={type}
        name={name}
        placeholder={placeholder}
        value={value}
        defaultValue={value === undefined ? defaultValue : undefined}
        onChange={(e) => onChange(e.target.value)}
        className="input-item"
        disabled={disabled}
        required={required}
      />
    </div>
  );
});
