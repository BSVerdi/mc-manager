import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    const result = await login(email, password);

    if (result) {
      navigate("/temp");
    } else {
      alert("Erro ao realizar o login");
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
            <div className="auth-input">
              <span>Email</span>
              <input
                type="email"
                placeholder="exemplo@email.com"
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="auth-input">
              <span>Senha</span>
              <input
                type="password"
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
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
