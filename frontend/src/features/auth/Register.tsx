import { MonitorCog } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import InputItem from "../../components/InputItem";
import SubmitButton from "../../components/SubmitButton";
import { useToast } from "../../components/toast";
import { useAuth } from "../../context/AuthContext";

export default function Register() {
  const [name, setName] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const { loading, register } = useAuth();
  const { success, error } = useToast();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (loading) return;

    const ok = await register(name, email, password);

    if (ok) {
      success("Registro realizado com sucesso", {
        id: "auth-success",
        duration: 2000,
      });
    } else {
      error("Erro ao registrar nova conta", {
        id: "auth-error",
        duration: 2000,
      });
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
            <InputItem title="Nome de usuário" type="text" required={true} onChange={setName} />
            <InputItem title="Email" type="email" required={true} placeholder="exemplo@email.com" onChange={setEmail} />
            <InputItem title="Senha" type="password" required={true} onChange={setPassword} />
            <SubmitButton type="submit" label={"Registrar"} loading={loading} />
          </form>
          <span className="auth-link">
            Já possui uma conta? <Link to="/login">Entre aqui</Link>
          </span>
        </section>
      </section>
    </>
  );
}
