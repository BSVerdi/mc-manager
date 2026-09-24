import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import InputItem from "../../components/InputItem";
import SubmitButton from "../../components/SubmitButton";
import { useToast } from "../../components/toast";
import { useAuth } from "../../context/AuthContext";

export default function Register() {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const { error, success } = useToast();

  const { loading, login } = useAuth();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (loading) return;

    const result = await login(email, password);

    if (!result) {
      error("Erro ao realizar login.", {
        id: "auth-error",
        duration: 2000,
      });
      return;
    }

    success("Login realizado com sucesso.", {
      id: "auth-success",
      duration: 2000,
    });
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
            <InputItem title="Email" type="email" placeholder="exemplo@email.com" onChange={setEmail} />
            <InputItem title="Senha" type="password" onChange={setPassword} />
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
