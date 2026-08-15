import { BrowserRouter, Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Home from "./pages/Home";
import Register from "./pages/Register";
import Temp from "./pages/Temp";
import { AuthProvider } from "./context/AuthContext";
import { PublicOnlyRoute } from "./components/PublicOnlyRoute";
import { PrivateRoute } from "./components/PrivateRoute";

export default function Router() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Route>
          <Route element={<PrivateRoute />}>
            <Route path="/" element={<Home />} />
            <Route path="/temp" element={<Temp />}></Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
