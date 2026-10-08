import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { InputItem } from "../../components/InputItem";
import { SubmitButton } from "../../components/SubmitButton";
import { useToast } from "../../components/toast";
import { useAuth } from "../../context/AuthContext";

export function Login() {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const { error, success } = useToast();

  const { loading, login } = useAuth();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (loading) return;

    const ok = await login(email, password);

    if (ok) {
      success("Login realizado com sucesso.", {
        id: "auth-success",
        duration: 2000,
      });
    } else {
      error("Erro ao realizar login.", {
        id: "auth-error",
        duration: 2000,
      });
    }
  };

  return (
    <>
      <section className="auth-container login">
        <section className="auth-app-title">
          <MonitorCog style={{ width: 48, height: 48 }} />
          <h1>MC-Manager</h1>
        </section>
        <section className="auth-content">
          <div className="auth-inputs-header">
            <h2>Login</h2>
            <span>Entre na sua conta para continuar</span>
          </div>
          <form onSubmit={handleSubmit} className="auth-inputs">
            <InputItem title="Email" type="email" required={true} placeholder="exemplo@email.com" onChange={setEmail} />
            <InputItem title="Senha" type="password" required={true} onChange={setPassword} />
            <SubmitButton type="submit" label={"Login"} loading={loading} />
          </form>
          <span className="auth-link">
            Não possui uma conta? <Link to="/register">Registre-se aqui</Link>
          </span>
        </section>
      </section>
    </>
  );
}
