import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

/* ============================
   NGROK BACKEND BASE URL
   ============================ */
const API_BASE = "https://nonresponsible-goodlier-kristan.ngrok-free.dev";

function Dashboard() {
  const navigate = useNavigate();

  // ----------------------------
  // FORM STATE
  // ----------------------------
  const [cpu, setCpu] = useState("");
  const [ram, setRam] = useState("");
  const [disk, setDisk] = useState("");
  const [mips, setMips] = useState("");

  // ----------------------------
  // DATA STATE
  // ----------------------------
  const [instances, setInstances] = useState([]);

  // ----------------------------
  // POLL INSTANCES
  // ----------------------------
  useEffect(() => {
    const poll = setInterval(() => {
      fetch(`${API_BASE}/api/instances`)
        .then(res => res.json())
        .then(setInstances)
        .catch(() => {});
    }, 2000);

    return () => clearInterval(poll);
  }, []);

  // ----------------------------
  // ACTIONS
  // ----------------------------
  const handleAllocate = async () => {
    if (!cpu || !ram || !disk || !mips) {
      alert("Fill all fields");
      return;
    }

    await fetch(`${API_BASE}/api/submit-task`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ cpu, ram, disk, mips }),
    });

    setCpu("");
    setRam("");
    setDisk("");
    setMips("");
  };

  const post = (url, body) =>
    fetch(`${API_BASE}${url}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

  return (
    <div style={styles.wrapper}>
      {/* HEADER */}
      <div style={styles.header}>
        ⚡ VM Allocator Dashboard
        <button
          style={styles.analyticsBtn}
          onClick={() => navigate("/analytics")}
        >
          📊 View Analytics
        </button>
      </div>

      <div style={styles.container}>
        {/* ALLOCATE */}
        <div style={styles.card}>
          <h3>➕ Allocate VM</h3>

          <input
            style={styles.input}
            placeholder="CPU (cores)"
            value={cpu}
            onChange={(e) => setCpu(e.target.value)}
          />
          <input
            style={styles.input}
            placeholder="RAM (MB)"
            value={ram}
            onChange={(e) => setRam(e.target.value)}
          />
          <input
            style={styles.input}
            placeholder="DISK (MB)"
            value={disk}
            onChange={(e) => setDisk(e.target.value)}
          />
          <input
            style={styles.input}
            placeholder="MIPS"
            value={mips}
            onChange={(e) => setMips(e.target.value)}
          />

          <button style={styles.button} onClick={handleAllocate}>
            Allocate
          </button>
        </div>

        {/* INSTANCES */}
        <div style={styles.cardWide}>
          <h3>🖥 Instances</h3>

          {instances.length === 0 ? (
            <p style={{ opacity: 0.7 }}>No active instances</p>
          ) : (
            <table style={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Host</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {instances.map((inst) => (
                  <tr key={inst.instanceId}>
                    <td>{inst.instanceId}</td>
                    <td>{inst.host}</td>
                    <td
                      style={{
                        fontWeight: "600",
                        color:
                          inst.status === "RUNNING"
                            ? "#22c55e"
                            : inst.status === "STOPPED"
                            ? "#facc15"
                            : "#ef4444",
                      }}
                    >
                      {inst.status}
                    </td>
                    <td>
                      {inst.status === "RUNNING" && (
                        <>
                          <button
                            style={styles.stopBtn}
                            onClick={() =>
                              post("/api/deallocate", {
                                instanceId: inst.instanceId,
                              })
                            }
                          >
                            Stop
                          </button>
                          <button
                            style={styles.killBtn}
                            onClick={() =>
                              post("/api/kill", {
                                instanceId: inst.instanceId,
                              })
                            }
                          >
                            Kill
                          </button>
                        </>
                      )}

                      {inst.status === "STOPPED" && (
                        <>
                          <button
                            style={styles.startBtn}
                            onClick={() =>
                              post("/api/start", {
                                instanceId: inst.instanceId,
                              })
                            }
                          >
                            Start
                          </button>
                          <button
                            style={styles.killBtn}
                            onClick={() =>
                              post("/api/kill", {
                                instanceId: inst.instanceId,
                              })
                            }
                          >
                            Kill
                          </button>
                        </>
                      )}

                      {inst.status === "TERMINATED" && (
                        <span style={{ opacity: 0.5 }}>Destroyed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

// ----------------------------
// STYLES
// ----------------------------
const styles = {
  wrapper: {
    minHeight: "100vh",
    background: "linear-gradient(135deg,#0f172a,#020617)",
    color: "white",
    fontFamily: "Segoe UI, sans-serif",
  },
  header: {
    height: "70px",
    padding: "0 20px",
    background: "linear-gradient(90deg,#2563eb,#1d4ed8)",
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    fontSize: "24px",
    fontWeight: "700",
  },
  analyticsBtn: {
    padding: "8px 14px",
    background: "#020617",
    color: "white",
    border: "1px solid #334155",
    borderRadius: "8px",
    cursor: "pointer",
    fontSize: "14px",
  },
  container: {
    padding: "30px",
    display: "flex",
    flexDirection: "column",
    gap: "24px",
    alignItems: "center",
  },
  card: {
    width: "420px",
    background: "rgba(255,255,255,0.08)",
    padding: "20px",
    borderRadius: "14px",
  },
  cardWide: {
    width: "90%",
    background: "rgba(255,255,255,0.08)",
    padding: "20px",
    borderRadius: "14px",
  },
  input: {
    width: "100%",
    padding: "10px",
    marginBottom: "10px",
    borderRadius: "8px",
    border: "1px solid #334155",
    background: "#020617",
    color: "white",
  },
  button: {
    width: "100%",
    padding: "10px",
    background: "#2563eb",
    border: "none",
    borderRadius: "8px",
    color: "white",
    fontWeight: "600",
    cursor: "pointer",
  },
  table: {
    width: "100%",
    textAlign: "center",
    borderCollapse: "collapse",
  },
  stopBtn: {
    background: "#facc15",
    border: "none",
    padding: "6px 10px",
    borderRadius: "6px",
    marginRight: "6px",
    cursor: "pointer",
  },
  startBtn: {
    background: "#22c55e",
    border: "none",
    padding: "6px 10px",
    borderRadius: "6px",
    marginRight: "6px",
    cursor: "pointer",
  },
  killBtn: {
    background: "#ef4444",
    border: "none",
    padding: "6px 10px",
    borderRadius: "6px",
    color: "white",
    cursor: "pointer",
  },
};

export default Dashboard;

