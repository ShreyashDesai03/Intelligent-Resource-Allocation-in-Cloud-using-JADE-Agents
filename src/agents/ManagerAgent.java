package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;

/**
 * ManagerAgent performs TRUE load-aware scheduling.
 * Scheduling decisions are based ONLY on real LOAD messages.
 * Backend is used ONLY for visualization and monitoring.
 */
public class ManagerAgent extends Agent {

    // -------------------------------
    // RESOURCE LOAD TABLE (REAL STATE)
    // -------------------------------
    private final Map<String, ResourceLoad> resourceLoads = new HashMap<>();

    // INSTANCE → AGENT
    private final Map<String, String> instanceToAgent = new HashMap<>();

    // AUTO-SCALED AGENTS
    private final Set<String> autoScaledAgents = new HashSet<>();

    // GLOBAL METRICS
    private int totalTasks = 0;
    private double totalExecutionTime = 0.0;

    private int nextAgentId = 3;

    @Override
    protected void setup() {
        System.out.println("ManagerAgent started.");

        /* ==================================================
           RECEIVE MESSAGES FROM RESOURCE AGENTS
           ================================================== */
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();

                // -------- REGISTER --------
                if (content.startsWith("REGISTER:")) {
                    System.out.println("✔ REGISTERED → " + sender);
                }

                // -------- LOAD UPDATE --------
                else if (content.startsWith("LOAD:")) {
                    ResourceLoad load = ResourceLoad.parseLoad(content);
                    resourceLoads.put(load.agent, load);

                    recomputeGlobalMetrics();
                    sendAgentLoadToBackend(load);
                }

                // -------- ALLOCATED --------
                else if (content.startsWith("ALLOCATED:")) {
                    String instanceId =
                            content.split(",")[0].split("=")[1];
                    instanceToAgent.put(instanceId, sender);
                    sendResultToBackend(content);
                }

                // -------- DEALLOCATED --------
                else if (content.startsWith("DEALLOCATED:")
                      || content.startsWith("KILLED:")) {
                    sendResultToBackend(content);
                }
            }
        });

        /* ==================================================
           POLL BACKEND FOR COMMANDS
           ================================================== */
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {

                String command = fetchCommandFromBackend();
                if (command == null) return;

                if (command.startsWith("ALLOCATE")) {
                    handleAllocate(command);
                } else {
                    handleLifecycle(command);
                }
            }
        });
    }

    // ==================================================
    // LOAD-AWARE ALLOCATION (CORE LOGIC)
    // ==================================================
    private void handleAllocate(String command) {

        if (resourceLoads.isEmpty()) {
            System.out.println("⏳ Waiting for resource LOAD data...");
            return;
        }

        VmRequest req = VmRequest.parse(command);

        ResourceLoad best = null;
        double bestScore = Double.MAX_VALUE;

        for (ResourceLoad r : resourceLoads.values()) {
            if (r.canFit(req)) {
                double score = r.loadScore();
                if (score < bestScore) {
                    bestScore = score;
                    best = r;
                }
            }
        }

        if (best != null) {
            System.out.println("🎯 Allocating to " + best.agent);
            sendCommand(best.agent, command);
            return;
        }

        // ---------------- AUTO-SCALING ----------------
        try {
            String newAgent = "ResourceAgent" + nextAgentId++;
            ContainerController cc = getContainerController();

            AgentController ac =
                    cc.createNewAgent(
                            newAgent,
                            "agents.ResourceAgent",
                            null);
            ac.start();

            autoScaledAgents.add(newAgent);
            System.out.println("⚡ Auto-scaled → " + newAgent);

        } catch (Exception e) {
            sendResultToBackend(
                    "FAILED: Auto-scale error - " + e.getMessage());
        }
    }

    // ==================================================
    // DEALLOCATE / KILL
    // ==================================================
    private void handleLifecycle(String command) {

        String instanceId = command.split(":")[1];
        String agent = instanceToAgent.get(instanceId);

        if (agent == null) {
            sendResultToBackend(
                    "FAILED: Unknown instance " + instanceId);
            return;
        }

        sendCommand(agent, command);

        if (command.startsWith("KILL")) {
            autoScaledAgents.remove(agent);
            resourceLoads.remove(agent);
            instanceToAgent.remove(instanceId);
        }
    }

    // ==================================================
    // GLOBAL METRICS
    // ==================================================
    private void recomputeGlobalMetrics() {

        int tasks = 0;
        double execSum = 0.0;

        for (ResourceLoad r : resourceLoads.values()) {
            tasks += r.tasksExecuted;
            execSum += r.avgExecTime * r.tasksExecuted;
        }

        totalTasks = tasks;
        totalExecutionTime = execSum;

        double avgExec =
                totalTasks == 0 ? 0 : totalExecutionTime / totalTasks;

        sendMetricsToBackend(totalTasks, avgExec);
    }

    // ==================================================
    // SEND AGENT LOAD TO BACKEND
    // ==================================================
    private void sendAgentLoadToBackend(ResourceLoad r) {
        try {
            URL url = new URL("http://localhost:5000/api/agent-loads");
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json =
                    "{ \"agent\": \"" + r.agent + "\"," +
                    "\"usedCPU\": " + r.usedCPU + "," +
                    "\"freeCPU\": " + r.freeCPU + "," +
                    "\"usedRAM\": " + r.usedRAM + "," +
                    "\"freeRAM\": " + r.freeRAM + "," +
                    "\"usedMIPS\": " + r.usedMIPS + "," +
                    "\"freeMIPS\": " + r.freeMIPS + "," +
                    "\"tasksExecuted\": " + r.tasksExecuted + "," +
                    "\"avgExecTime\": " + r.avgExecTime + " }";

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            conn.getResponseCode();
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("❌ Agent-load backend error: " + e.getMessage());
        }
    }

    // ==================================================
    // SEND COMMAND
    // ==================================================
    private void sendCommand(String agent, String command) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID(agent, AID.ISLOCALNAME));
        msg.setContent(command);
        send(msg);
    }

    // ==================================================
    // BACKEND COMMUNICATION
    // ==================================================
    private String fetchCommandFromBackend() {
        try {
            URL url =
                    new URL("http://localhost:5000/api/fetch-command");
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()));
            String line = br.readLine();
            conn.disconnect();

            if (line == null || line.contains("null")) return null;

            return line.replace("{\"command\":\"", "")
                       .replace("\"}", "")
                       .trim();

        } catch (Exception e) {
            return null;
        }
    }

    private void sendResultToBackend(String result) {
        try {
            URL url =
                    new URL("http://localhost:5000/api/sim-result");
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "{ \"result\": \"" + result + "\" }";

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            conn.getResponseCode();
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("❌ Backend error: " + e.getMessage());
        }
    }

    private void sendMetricsToBackend(int totalTasks, double avgExecTime) {
        try {
            URL url =
                    new URL("http://localhost:5000/api/metrics");
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json =
                    "{ \"totalTasks\": " + totalTasks +
                    ", \"avgExecutionTime\": " + avgExecTime + " }";

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            conn.getResponseCode();
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("❌ Metrics backend error: " + e.getMessage());
        }
    }

    // ==================================================
    // SUPPORT CLASSES
    // ==================================================
    private static class ResourceLoad {
        String agent;
        int usedCPU, usedRAM, usedMIPS;
        int freeCPU, freeRAM, freeMIPS;
        int tasksExecuted;
        double avgExecTime;

        boolean canFit(VmRequest r) {
            return freeCPU >= r.cpu &&
                   freeRAM >= r.ram &&
                   freeMIPS >= r.mips;
        }

        double loadScore() {
            return usedCPU +
                   (usedRAM / 1000.0) +
                   (usedMIPS / 1000.0);
        }

        static ResourceLoad parseLoad(String content) {
            ResourceLoad r = new ResourceLoad();
            String payload = content.replace("LOAD:", "");
            String[] parts = payload.split(",");

            for (String p : parts) {
                String[] kv = p.split("=");
                switch (kv[0]) {
                    case "AGENT": r.agent = kv[1]; break;
                    case "USED_CPU": r.usedCPU = Integer.parseInt(kv[1]); break;
                    case "USED_RAM": r.usedRAM = Integer.parseInt(kv[1]); break;
                    case "USED_MIPS": r.usedMIPS = Integer.parseInt(kv[1]); break;
                    case "FREE_CPU": r.freeCPU = Integer.parseInt(kv[1]); break;
                    case "FREE_RAM": r.freeRAM = Integer.parseInt(kv[1]); break;
                    case "FREE_MIPS": r.freeMIPS = Integer.parseInt(kv[1]); break;
                    case "TASKS": r.tasksExecuted = Integer.parseInt(kv[1]); break;
                    case "AVG_EXEC_TIME": r.avgExecTime = Double.parseDouble(kv[1]); break;
                }
            }
            return r;
        }
    }

    private static class VmRequest {
        int cpu, ram, mips;

        static VmRequest parse(String cmd) {
            VmRequest r = new VmRequest();
            String spec = cmd.split(":", 2)[1];
            String[] p = spec.split(",");
            r.cpu = Integer.parseInt(p[0].split("=")[1]);
            r.ram = Integer.parseInt(p[1].split("=")[1]);
            r.mips = Integer.parseInt(p[3].split("=")[1]);
            return r;
        }
    }
}
