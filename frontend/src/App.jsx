import { Navigate, Route, Routes } from "react-router-dom";
import TopBar from "./components/TopBar";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import SlotsPage from "./pages/SlotsPage";
import BookPage from "./pages/BookPage";
import ConfirmationPage from "./pages/ConfirmationPage";
import MyAppointmentsPage from "./pages/MyAppointmentsPage";

export default function App() {
  return (
    <div className="app-shell">
      <TopBar />
      <Routes>
        <Route path="/" element={<Navigate to="/slots" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/slots"
          element={
            <ProtectedRoute>
              <SlotsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/book/:slotId"
          element={
            <ProtectedRoute>
              <BookPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/confirmation/:appointmentId"
          element={
            <ProtectedRoute>
              <ConfirmationPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-appointments"
          element={
            <ProtectedRoute>
              <MyAppointmentsPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/slots" replace />} />
      </Routes>
    </div>
  );
}
