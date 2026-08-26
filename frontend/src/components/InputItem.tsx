type InputItemProps = {
  type: string;
  title: string;
  placeHolder?: string;
  defaultValue?: string;
  setFunction: (value: any) => void;
  isRequired?: boolean;
};

export default function Input({
  type,
  title,
  placeHolder,
  defaultValue,
  setFunction: stateFunction,
}: InputItemProps) {
  return (
    <div className="input-item-container">
      <span className="input-item-title">{title}</span>
      <input
        type={type}
        placeholder={placeHolder}
        defaultValue={defaultValue}
        onChange={(e) => stateFunction(e.target.value)}
        className="input-item"
      />
    </div>
  );
}
