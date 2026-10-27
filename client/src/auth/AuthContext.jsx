import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api } from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setUser(await api.get("/api/auth/me"));
    } catch {
      setUser(null);
    } finally {
      setReady(true);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const login = async (email, password) => {
    const me = await api.post("/api/auth/login", { email, password });
    setUser(me);
    return me;
  };

  const register = async (kind, body) => {
    const me = await api.post(`/api/auth/register/${kind}`, body);
    setUser(me);
    return me;
  };

  const logout = async () => {
    await api.post("/api/auth/logout");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, ready, login, register, logout, refresh }}>{children}</AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

export const homeFor = (role) =>
  ({ PATIENT: "/app/patient", DOCTOR: "/app/doctor", ADMIN: "/app/admin" })[role] || "/";
