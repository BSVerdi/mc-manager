type InputItemProps = {
  type: string;
  title: string;
  placeHolder?: string;
  defaultValue?: string;
  setFunction: (value: any) => void;
  isRequired?: boolean;
  disabled?: boolean;
};

export default function InputItem({
  type,
  title,
  placeHolder,
  defaultValue,
  setFunction,
  disabled,
}: InputItemProps) {
  return (
    <div className="input-item-container">
      <span className="input-item-title">{title}</span>
      <input
        type={type}
        placeholder={placeHolder}
        defaultValue={defaultValue}
        onChange={(e) => setFunction(e.target.value)}
        className="input-item"
        disabled={disabled}
      />
    </div>
  );
}
