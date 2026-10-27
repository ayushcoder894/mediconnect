import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { homeFor, useAuth } from "./auth/AuthContext";
import { Spinner } from "./components/ui";
import PublicLayout from "./components/PublicLayout";
import AppLayout from "./components/AppLayout";

import Home from "./pages/public/Home";
import Doctors from "./pages/public/Doctors";
import DoctorDetail from "./pages/public/DoctorDetail";
import LabTests from "./pages/public/LabTests";
import BloodOrgans from "./pages/public/BloodOrgans";
import Login from "./pages/public/Login";
import Register from "./pages/public/Register";
import NotFound from "./pages/public/NotFound";

import PatientDashboard from "./pages/patient/Dashboard";
import MyAppointments from "./pages/patient/Appointments";
import MyLabTests from "./pages/patient/LabBookings";
import RiskCheck from "./pages/patient/RiskCheck";
import Assistant from "./pages/patient/Assistant";
import Prescriptions from "./pages/patient/Prescriptions";
import Profile from "./pages/patient/Profile";

import DoctorDashboard from "./pages/doctor/Dashboard";
import PatientRecord from "./pages/doctor/PatientRecord";
import Practice from "./pages/doctor/Practice";

import AdminDashboard from "./pages/admin/Dashboard";
import AdminPeople from "./pages/admin/People";
import LabDesk from "./pages/admin/LabDesk";
import Stock from "./pages/admin/Stock";
import CheckIn from "./pages/admin/CheckIn";
import Logs from "./pages/admin/Logs";

function RequireRole({ role, children }) {
  const { user, ready } = useAuth();
  const location = useLocation();
  if (!ready) return <Spinner className="min-h-screen" />;
  if (!user) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />;
  if (role && user.role !== role) return <Navigate to={homeFor(user.role)} replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/doctors" element={<Doctors />} />
        <Route path="/doctors/:id" element={<DoctorDetail />} />
        <Route path="/lab-tests" element={<LabTests />} />
        <Route path="/blood-organs" element={<BloodOrgans />} />
        <Route path="*" element={<NotFound />} />
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route path="/app/patient" element={<RequireRole role="PATIENT"><AppLayout /></RequireRole>}>
        <Route index element={<PatientDashboard />} />
        <Route path="appointments" element={<MyAppointments />} />
        <Route path="labs" element={<MyLabTests />} />
        <Route path="risk" element={<RiskCheck />} />
        <Route path="assistant" element={<Assistant />} />
        <Route path="prescriptions" element={<Prescriptions />} />
        <Route path="profile" element={<Profile />} />
      </Route>

      <Route path="/app/doctor" element={<RequireRole role="DOCTOR"><AppLayout /></RequireRole>}>
        <Route index element={<DoctorDashboard />} />
        <Route path="patients/:id" element={<PatientRecord />} />
        <Route path="practice" element={<Practice />} />
      </Route>

      <Route path="/app/admin" element={<RequireRole role="ADMIN"><AppLayout /></RequireRole>}>
        <Route index element={<AdminDashboard />} />
        <Route path="people" element={<AdminPeople />} />
        <Route path="lab" element={<LabDesk />} />
        <Route path="stock" element={<Stock />} />
        <Route path="check-in" element={<CheckIn />} />
        <Route path="logs" element={<Logs />} />
      </Route>
    </Routes>
  );
}
