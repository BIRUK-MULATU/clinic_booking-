import { Navigate, Route, Routes } from "react-router-dom";
import TopBar from "./components/TopBar";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import SlotsPage from "./pages/SlotsPage";
import BookPage from "./pages/BookPage";
import ConfirmationPage from "./pages/ConfirmationPage";
import MyAppointmentsPage from "./pages/MyAppointmentsPage";
import DoctorsPage from "./pages/DoctorsPage";
import AvailabilityPage from "./pages/AvailabilityPage";
import QueuePage from "./pages/QueuePage";
import VisitRecordPage from "./pages/VisitRecordPage";
import PatientsPage from "./pages/PatientsPage";
import ManageSlotsPage from "./pages/ManageSlotsPage";
import Watermark from "./components/Watermark";
import { useAuth } from "./context/AuthContext";

function HomeRedirect() {
  const { patient } = useAuth();
  return <Navigate to={patient?.role === "ADMIN" ? "/queue" : "/slots"} replace />;
}

export default function App() {
  const { patient } = useAuth();

  return (
    <div className="app-shell">
      {patient?.role === "PATIENT" && <Watermark />}
      <TopBar />
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/slots"
          element={
            <ProtectedRoute role="PATIENT">
              <SlotsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/book/:slotId"
          element={
            <ProtectedRoute role="PATIENT">
              <BookPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/confirmation/:appointmentId"
          element={
            <ProtectedRoute role="PATIENT">
              <ConfirmationPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-appointments"
          element={
            <ProtectedRoute role="PATIENT">
              <MyAppointmentsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/doctors"
          element={
            <ProtectedRoute role="ADMIN">
              <DoctorsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/doctors/:doctorId/availability"
          element={
            <ProtectedRoute role="ADMIN">
              <AvailabilityPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/queue"
          element={
            <ProtectedRoute role="ADMIN">
              <QueuePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/appointments/:appointmentId/visit-record"
          element={
            <ProtectedRoute role="ADMIN">
              <VisitRecordPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/patients"
          element={
            <ProtectedRoute role="ADMIN">
              <PatientsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/manage-slots"
          element={
            <ProtectedRoute role="ADMIN">
              <ManageSlotsPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<HomeRedirect />} />
      </Routes>
    </div>
  );
}
