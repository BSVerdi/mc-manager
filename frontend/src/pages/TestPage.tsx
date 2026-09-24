import { useState } from "react";
import SubmitButton from "../components/SubmitButton";

export default function TestPage() {
  const [loading, setLoading] = useState<boolean>(false);

  return (
    <div className="test-page-container">
      <SubmitButton label="Submit" loading={loading} onClick={() => setLoading(!loading)} />
    </div>
  );
}
