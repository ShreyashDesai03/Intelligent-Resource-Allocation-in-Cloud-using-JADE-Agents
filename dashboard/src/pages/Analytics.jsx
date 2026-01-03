import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  Tooltip,
  BarChart,
  Bar,
} from "recharts";

function Analytics() {
  const navigate = useNavigate();

  // ----------------------------
  // STATE
  // ----------------------------
  const [instances, setInstances] = useState([]);
  const [history, setHistory] = useState([]);
  const [metrics, setMetrics] = useState({
    totalTasks: 0,
    avgExecutionTime: 0,
  });
  const [trend, setTrend] = useState([]);
  const [agentLoads, setAgentLoads] = useState([]);

  // ----------------------------
  // POLLING
  // ----------------------------
  useEffect(() => {
    const poll = setInterval(() => {
      fetch("http://localhost:5000/api/instances")
        .then(res => res.json())
        .then(setInstances)
        .catch(() => {});

      fetch("http://localhost:5000/api/history")
        .then(res => res.json())
        .then(setHistory)
        .catch(() => {});

      fetch("http://localhost:5000/api/metrics")
        .then(res => res.json())
        .then(data => {
          const safe = {
            totalTasks: Number(data?.totalTasks) || 0,
            avgExecutionTime: Number(data?.avgExecutionTime) || 0,
          };

          setMetrics(safe);
          setTrend(prev => [
            ...prev.slice(-9),
            {
              time: new Date().toLocaleTimeString(),
              avg: safe.avgExecutionTime,
            },
          ]);
        })
        .catch(() => {});

      fetch("http://localhost:5000/api/agent-loads")
        .then(res => res.json())
        .then(data => setAgentLoads(Array.isArray(data) ? data : []))
        .catch(() => {});
    }, 2000);

    return () => clearInterval(poll);
  }, []);

  // ----------------------------
  // CHART DATA
  // ----------------------------
  const statusData = [
    { name: "RUNNING", value: instances.filter(i => i.status === "RUNNING").length },
    { name: "STOPPED", value: instances.filter(i => i.status === "STOPPED").length },
    { name: "TERMINATED", value: instances.filter(i => i.status === "TERMINATED").length },
  ];

  const cpuChartData = agentLoads.map(a => ({
    agent: a.agent,
    usedCPU: Number(a.usedCPU) || 0,
    freeCPU: Number(a.freeCPU) || 0,
  }));

  const ramChartData = agentLoads.map(a => ({
    agent: a.agent,
    usedRAM: Number(a.usedRAM) || 0,
    freeRAM: Number(a.freeRAM) || 0,
  }));

  const mipsChartData = agentLoads.map(a => ({
    agent: a.agent,
    usedMIPS: Number(a.usedMIPS) || 0,
    freeMIPS: Number(a.freeMIPS) || 0,
  }));

  const COLORS = ["#22c55e", "#facc15", "#ef4444"];

  return (
    <div style={styles.wrapper}>
      {/* HEADER */}
      <div style={styles.header}>
        📊 Analytics & Monitoring
        <button style={styles.backBtn} onClick={() => navigate("/")}>
          ← Back
        </button>
      </div>

      <div style={styles.container}>
        {/* METRICS */}
        <div style={styles.card}>
          <h3>📈 System Metrics</h3>
          <p><b>Total Tasks:</b> {metrics.totalTasks}</p>
          <p><b>Avg Execution Time:</b> {metrics.avgExecutionTime.toFixed(2)}</p>
        </div>

        {/* EXECUTION TREND */}
        <div style={styles.card}>
          <h3>⏱ Execution Trend</h3>
          <LineChart width={360} height={200} data={trend}>
            <XAxis dataKey="time" hide />
            <YAxis />
            <Tooltip />
            <Line dataKey="avg" stroke="#22c55e" strokeWidth={2} dot={false} />
          </LineChart>
        </div>

        {/* VM STATUS */}
        <div style={styles.cardWide}>
          <h3>🧩 VM Status Distribution</h3>
          <PieChart width={360} height={240}>
            <Pie data={statusData} dataKey="value" outerRadius={90} label>
              {statusData.map((_, i) => (
                <Cell key={i} fill={COLORS[i]} />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </div>

        {/* CPU LOAD */}
        <div style={styles.cardWide}>
          <h3>⚙ CPU Utilization</h3>
          <BarChart width={520} height={220} data={cpuChartData}>
            <XAxis dataKey="agent" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="usedCPU" stackId="cpu" fill="#ef4444" />
            <Bar dataKey="freeCPU" stackId="cpu" fill="#22c55e" />
          </BarChart>
        </div>

        {/* RAM LOAD */}
        <div style={styles.cardWide}>
          <h3>🧠 RAM Utilization</h3>
          <BarChart width={520} height={220} data={ramChartData}>
            <XAxis dataKey="agent" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="usedRAM" stackId="ram" fill="#ef4444" />
            <Bar dataKey="freeRAM" stackId="ram" fill="#22c55e" />
          </BarChart>
        </div>

        {/* MIPS LOAD */}
        <div style={styles.cardWide}>
          <h3>⚡ MIPS Utilization</h3>
          <BarChart width={520} height={220} data={mipsChartData}>
            <XAxis dataKey="agent" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="usedMIPS" stackId="mips" fill="#ef4444" />
            <Bar dataKey="freeMIPS" stackId="mips" fill="#22c55e" />
          </BarChart>
        </div>

        {/* AGENT LOAD TABLE */}
        <div style={styles.cardWide}>
          <h3>🧠 Resource Agent Load Table</h3>
          <table style={styles.table}>
            <thead>
              <tr>
                <th>Agent</th>
                <th>Used CPU</th>
                <th>Free CPU</th>
                <th>Used RAM</th>
                <th>Free RAM</th>
                <th>Used MIPS</th>
                <th>Free MIPS</th>
                <th>Tasks</th>
              </tr>
            </thead>
            <tbody>
              {agentLoads.map((a, i) => (
                <tr key={i}>
                  <td>{a.agent}</td>
                  <td>{a.usedCPU}</td>
                  <td>{a.freeCPU}</td>
                  <td>{a.usedRAM}</td>
                  <td>{a.freeRAM}</td>
                  <td>{a.usedMIPS}</td>
                  <td>{a.freeMIPS}</td>
                  <td>{a.tasksExecuted}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* HISTORY */}
        <div style={styles.cardWideScrollable}>
          <h3>📜 Last 5 Events</h3>
          {history.map((h, i) => (
            <div key={i} style={styles.historyItem}>
              {new Date(h.timestamp).toLocaleTimeString()} — {h.result}
            </div>
          ))}
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
    background: "linear-gradient(135deg,#020617,#0f172a)",
    color: "white",
    fontFamily: "Segoe UI, sans-serif",
  },
  header: {
    height: "70px",
    padding: "0 20px",
    background: "linear-gradient(90deg,#1d4ed8,#2563eb)",
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    fontSize: "22px",
    fontWeight: "700",
  },
  backBtn: {
    padding: "8px 14px",
    background: "#020617",
    color: "white",
    border: "1px solid #334155",
    borderRadius: "8px",
    cursor: "pointer",
  },
  container: {
    padding: "30px",
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "24px",
  },
  card: {
    background: "rgba(255,255,255,0.08)",
    padding: "20px",
    borderRadius: "14px",
  },
  cardWide: {
    gridColumn: "1 / span 2",
    background: "rgba(255,255,255,0.08)",
    padding: "20px",
    borderRadius: "14px",
  },
  cardWideScrollable: {
    gridColumn: "1 / span 2",
    background: "rgba(255,255,255,0.08)",
    padding: "20px",
    borderRadius: "14px",
    maxHeight: "260px",
    overflowY: "auto",
  },
  historyItem: {
    background: "rgba(255,255,255,0.1)",
    padding: "8px",
    borderRadius: "6px",
    marginBottom: "6px",
    fontSize: "14px",
  },
  table: {
    width: "100%",
    borderCollapse: "collapse",
    textAlign: "center",
  },
};

export default Analytics;
