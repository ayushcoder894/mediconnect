import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api, setToken } from "../api/client";

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

  const signedIn = ({ token, ...me }) => {
    setToken(token);
    setUser(me);
    return me;
  };

  const login = async (email, password) => signedIn(await api.post("/api/auth/login", { email, password }));

  const register = async (kind, body) => signedIn(await api.post(`/api/auth/register/${kind}`, body));

  const logout = async () => {
    try {
      await api.post("/api/auth/logout");
    } finally {
      setToken(null);
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, ready, login, register, logout, refresh }}>{children}</AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

export const homeFor = (role) =>
  ({ PATIENT: "/app/patient", DOCTOR: "/app/doctor", ADMIN: "/app/admin" })[role] || "/";
