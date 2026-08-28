import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import InputItem from "../components/InputItem";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const { login } = useAuth();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    const result = await login(email, password);

    if (!result) {
      alert("Erro ao realizar o login.");
    }
  };

  return (
    <main>
      <section className="auth-container login">
        <section className="auth-app-title">
          <MonitorCog style={{ width: 64, height: 64 }} />
          <h1>MC-Manager</h1>
        </section>
        <section className="auth-content">
          <div className="auth-inputs-header">
            <h2>Login</h2>
            <span>Entre na sua conta para continuar</span>
          </div>
          <form onSubmit={handleSubmit} className="auth-inputs">
            <InputItem
              title="Email"
              type="email"
              placeHolder="exemplo@email.com"
              setFunction={setEmail}
            />
            <InputItem
              title="Senha"
              type="password"
              setFunction={setPassword}
            />
            <button className="auth-btn" type="submit">
              Login
            </button>
          </form>
          <span className="auth-link">
            Não possui uma conta? <Link to="/register">Registre-se aqui</Link>
          </span>
        </section>
      </section>
    </main>
  );
}
