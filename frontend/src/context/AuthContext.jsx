import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api } from "../api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .session()
      .then(({ ok, data }) => setPatient(ok ? data : null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (username, password) => {
    const { ok, data } = await api.login(username, password);
    if (ok) {
      setPatient(data);
      return { ok: true };
    }
    return { ok: false, message: data?.message || "Invalid username or password." };
  }, []);

  const logout = useCallback(async () => {
    await api.logout();
    setPatient(null);
  }, []);

  return (
    <AuthContext.Provider value={{ patient, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
