import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export default function Register() {
  const [name, setName] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const navigate = useNavigate();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
  };

  return (
    <main>
      <section className="auth-container register">
        <section className="auth-app-title">
          <MonitorCog style={{ width: 64, height: 64 }} />
          <h1>MC-Manager</h1>
        </section>
        <section className="auth-content">
          <div className="auth-inputs-header">
            <h2>Registro</h2>
            <span>Registre uma conta para continuar</span>
          </div>
          <form onSubmit={handleSubmit} className="auth-inputs">
            <div className="auth-input">
              <span>Nome de usuário</span>
              <input type="text" onChange={(e) => setName(e.target.value)} />
            </div>
            <div className="auth-input">
              <span>Email</span>
              <input
                type="email"
                placeholder="exemplo@email.com"
                onChange={(e) => {
                  setEmail(e.target.value);
                }}
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
              Registrar
            </button>
          </form>
          <span className="auth-link">
            Já possui uma conta? <Link to="/login">Entre aqui</Link>
          </span>
        </section>
      </section>
    </main>
  );
}
