import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children, role }) {
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

  if (role && patient.role !== role) {
    return <Navigate to={patient.role === "ADMIN" ? "/queue" : "/slots"} replace />;
  }

  return children;
}
