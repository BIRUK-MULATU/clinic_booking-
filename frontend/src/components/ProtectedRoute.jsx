import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children }) {
  const { patient, loading } = useAuth();

  if (loading) {
    return (
      <div className="loading-screen">
        <span className="spinner" />
        Loading…
      </div>
    );
  }

  if (!patient) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
