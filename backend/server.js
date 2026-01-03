const express = require("express");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

// ------------------------------------------
// STATE
// ------------------------------------------

// Single-command queue (JADE polls every second)
let pendingCommand = null;

// instanceId -> instance info
let activeInstances = {};

// lifecycle history (bounded)
let history = [];

// ------------------------------------------
// METRICS STATE (FROM MANAGER AGENT)
// ------------------------------------------
let metrics = {
  totalTasks: 0,
  avgExecutionTime: 0,
};

// ------------------------------------------
// AGENT LOAD STATE (REAL LOAD DATA)
// ------------------------------------------
// agentName -> load object
let agentLoads = {};

// ------------------------------------------
// HELPER: COMMAND BUSY CHECK
// ------------------------------------------
function ensureFree(res) {
  if (pendingCommand) {
    res.status(429).json({
      error: "System busy, try again shortly",
    });
    return false;
  }
  return true;
}

// ------------------------------------------
// SUBMIT TASK → ALLOCATE INSTANCE
// ------------------------------------------
app.post("/api/submit-task", (req, res) => {
  const { cpu, ram, disk, mips } = req.body;

  if (!cpu || !ram || !disk || !mips) {
    return res.status(400).json({ error: "All fields are required" });
  }

  if (!ensureFree(res)) return;

  pendingCommand = `ALLOCATE:CPU=${cpu},RAM=${ram},DISK=${disk},MIPS=${mips}`;
  console.log("🆕 ALLOCATE command created:", pendingCommand);

  res.json({ status: "ALLOCATE_SENT" });
});

// ------------------------------------------
// DEALLOCATE INSTANCE (STOP)
// ------------------------------------------
app.post("/api/deallocate", (req, res) => {
  const { instanceId } = req.body;
  if (!instanceId) {
    return res.status(400).json({ error: "instanceId required" });
  }

  if (!ensureFree(res)) return;

  pendingCommand = `DEALLOCATE:${instanceId}`;
  console.log("🛑 DEALLOCATE command created:", pendingCommand);

  res.json({ status: "DEALLOCATE_SENT", instanceId });
});

// ------------------------------------------
// START INSTANCE (RESTART)
// ------------------------------------------
app.post("/api/start", (req, res) => {
  const { instanceId } = req.body;
  if (!instanceId) {
    return res.status(400).json({ error: "instanceId required" });
  }

  if (!ensureFree(res)) return;

  pendingCommand = `START:${instanceId}`;
  console.log("▶ START command created:", pendingCommand);

  res.json({ status: "START_SENT", instanceId });
});

// ------------------------------------------
// KILL INSTANCE (TERMINATE)
// ------------------------------------------
app.post("/api/kill", (req, res) => {
  const { instanceId } = req.body;
  if (!instanceId) {
    return res.status(400).json({ error: "instanceId required" });
  }

  if (!ensureFree(res)) return;

  pendingCommand = `KILL:${instanceId}`;
  console.log("💀 KILL command created:", pendingCommand);

  res.json({ status: "KILL_SENT", instanceId });
});

// ------------------------------------------
// JADE FETCHES COMMAND
// ------------------------------------------
app.get("/api/fetch-command", (req, res) => {
  if (!pendingCommand) {
    return res.json({ command: null });
  }

  const cmd = pendingCommand;
  pendingCommand = null;

  console.log("🟢 JADE fetched command:", cmd);
  res.json({ command: cmd });
});

// ------------------------------------------
// JADE SENDS RESULT (LIFECYCLE EVENTS)
// ------------------------------------------
app.post("/api/sim-result", (req, res) => {
  const { result } = req.body;
  if (!result || typeof result !== "string") {
    return res.status(400).json({ error: "Invalid result format" });
  }

  console.log("📥 JADE RESULT:", result);

  try {
    if (result.startsWith("ALLOCATED:")) {
      const parts = result.replace("ALLOCATED:", "").split(",");

      const instanceId = parts[0].split("=")[1];
      const vmId = parts[1].split("=")[1];
      const host = parts[2].split("=")[1];

      activeInstances[instanceId] = {
        instanceId,
        vmId,
        host,
        status: "RUNNING",
        startedAt: new Date(),
      };
    }

    else if (result.startsWith("DEALLOCATED:")) {
      const instanceId = result.split("INSTANCE=")[1]?.split(",")[0];
      if (activeInstances[instanceId]) {
        activeInstances[instanceId].status = "STOPPED";
        activeInstances[instanceId].stoppedAt = new Date();
      }
    }

    else if (result.startsWith("STARTED:")) {
      const instanceId = result.split("INSTANCE=")[1]?.split(",")[0];
      if (activeInstances[instanceId]) {
        activeInstances[instanceId].status = "RUNNING";
        activeInstances[instanceId].restartedAt = new Date();
      }
    }

    else if (result.startsWith("KILLED:")) {
      const instanceId = result.split("INSTANCE=")[1]?.split(",")[0];
      if (activeInstances[instanceId]) {
        activeInstances[instanceId].status = "TERMINATED";
        activeInstances[instanceId].terminatedAt = new Date();
      }
    }
  } catch (err) {
    console.error("❌ Result parse error:", err.message);
  }

  history.push({
    timestamp: new Date(),
    result,
  });

  if (history.length > 5) history.shift();

  res.json({ status: "stored" });
});

// ------------------------------------------
// METRICS FROM MANAGER AGENT
// ------------------------------------------
app.post("/api/metrics", (req, res) => {
  const { totalTasks, avgExecutionTime } = req.body;

  if (Number.isFinite(totalTasks)) {
    metrics.totalTasks = totalTasks;
  }
  if (Number.isFinite(avgExecutionTime)) {
    metrics.avgExecutionTime = avgExecutionTime;
  }

  console.log("📊 METRICS UPDATED:", metrics);
  res.json({ status: "metrics-updated" });
});

// ------------------------------------------
// AGENT LOADS FROM MANAGER AGENT
// ------------------------------------------
app.post("/api/agent-loads", (req, res) => {
  const load = req.body;

  if (!load || !load.agent) {
    return res.status(400).json({ error: "Invalid load data" });
  }

  agentLoads[load.agent] = {
    agent: load.agent,
    usedCPU: Number(load.usedCPU) || 0,
    freeCPU: Number(load.freeCPU) || 0,
    usedRAM: Number(load.usedRAM) || 0,
    freeRAM: Number(load.freeRAM) || 0,
    usedMIPS: Number(load.usedMIPS) || 0,
    freeMIPS: Number(load.freeMIPS) || 0,
    tasksExecuted: Number(load.tasksExecuted) || 0,
    avgExecTime: Number(load.avgExecTime) || 0,
    timestamp: new Date(),
  };

  res.json({ status: "agent-load-updated" });
});

// ------------------------------------------
// FRONTEND ENDPOINTS
// ------------------------------------------
app.get("/api/instances", (req, res) => {
  res.json(Object.values(activeInstances));
});

app.get("/api/history", (req, res) => {
  res.json(history);
});

app.get("/api/metrics", (req, res) => {
  res.json({
    totalTasks: metrics.totalTasks || 0,
    avgExecutionTime: metrics.avgExecutionTime || 0,
  });
});

app.get("/api/agent-loads", (req, res) => {
  res.json(Object.values(agentLoads));
});

// ------------------------------------------
// START SERVER
// ------------------------------------------
app.listen(5000, () => {
  console.log("🚀 Backend running at http://localhost:5000");
});
