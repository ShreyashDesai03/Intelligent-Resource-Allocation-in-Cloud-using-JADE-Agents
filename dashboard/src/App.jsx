import { BrowserRouter, Routes, Route } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import Analytics from "./pages/Analytics";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Main VM Control Dashboard */}
        <Route path="/" element={<Dashboard />} />

        {/* Analytics & Monitoring */}
        <Route path="/analytics" element={<Analytics />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
