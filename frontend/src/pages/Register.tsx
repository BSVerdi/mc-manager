import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import InputItem from "../components/InputItem";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const [name, setName] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const { register } = useAuth();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    const result = await register(name, email, password);

    if (!result) {
      alert("Erro ao registrar a conta");
    }
  };

  return (
    <>
      <section className="auth-container register">
        <section className="auth-app-title">
          <MonitorCog style={{ width: 48, height: 48 }} />
          <h1>MC-Manager</h1>
        </section>
        <section className="auth-content">
          <div className="auth-inputs-header">
            <h2>Registro</h2>
            <span>Registre uma conta para continuar</span>
          </div>
          <form onSubmit={handleSubmit} className="auth-inputs">
            <InputItem
              title="Nome de usuário"
              type="text"
              setFunction={setName}
            />
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
              Registrar
            </button>
          </form>
          <span className="auth-link">
            Já possui uma conta? <Link to="/login">Entre aqui</Link>
          </span>
        </section>
      </section>
    </>
  );
}
