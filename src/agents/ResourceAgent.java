package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import cloud.CloudSimEngine;
import cloud.CloudSimEngine.TaskResult;

import java.util.*;

public class ResourceAgent extends Agent {

    // -------------------------------
    // TOTAL HOST CAPACITY
    // -------------------------------
    private static final int TOTAL_CPU = 8;
    private static final int TOTAL_RAM = 16000;
    private static final int TOTAL_MIPS = 25000;

    // -------------------------------
    // CURRENT USAGE
    // -------------------------------
    private int usedCPU = 0;
    private int usedRAM = 0;
    private int usedMIPS = 0;

    // -------------------------------
    // VM TRACKING
    // -------------------------------
    private final Map<String, VmInfo> runningVMs = new HashMap<>();

    // -------------------------------
    // METRICS
    // -------------------------------
    private int tasksExecuted = 0;
    private double totalExecutionTime = 0.0;

    private CloudSimEngine cloudSimEngine;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");

        cloudSimEngine = new CloudSimEngine();

        // REGISTER
        ACLMessage register = new ACLMessage(ACLMessage.INFORM);
        register.addReceiver(new AID("ManagerAgent", AID.ISLOCALNAME));
        register.setContent(
                "REGISTER:" + getLocalName() +
                ":CPU=" + TOTAL_CPU +
                ",RAM=" + TOTAL_RAM +
                ",MIPS=" + TOTAL_MIPS
        );
        send(register);

        // ✅ SEND INITIAL LOAD
        sendLoadToManager();

        addBehaviour(new ResourceBehaviour());
    }

    // ==================================================
    // MAIN BEHAVIOUR
    // ==================================================
    private class ResourceBehaviour extends CyclicBehaviour {

        @Override
        public void action() {

            ACLMessage msg = receive();
            if (msg == null) {
                block();
                return;
            }

            String content = msg.getContent();

            // -------- ALLOCATE --------
            if (content.startsWith("ALLOCATE")) {

                VmRequest req = VmRequest.parse(content.split(":", 2)[1]);

                if (!canFit(req)) {
                    reply(msg, ACLMessage.REFUSE, "INSUFFICIENT_RESOURCES");
                    return;
                }

                String instanceId = "i-" + UUID.randomUUID().toString().substring(0, 8);

                try {
                    TaskResult result = cloudSimEngine.createAndRunVm(content.split(":", 2)[1]);

                    usedCPU += req.cpu;
                    usedRAM += req.ram;
                    usedMIPS += req.mips;

                    runningVMs.put(instanceId, new VmInfo(instanceId, result.vmId, req));

                    tasksExecuted++;
                    totalExecutionTime += result.executionTime;

                    sendLoadToManager();

                    reply(msg, ACLMessage.INFORM,
                            "ALLOCATED:INSTANCE=" + instanceId +
                            ",VM_ID=" + result.vmId +
                            ",HOST=" + getLocalName());

                } catch (Exception e) {
                    reply(msg, ACLMessage.FAILURE, "CloudSim error");
                }
            }

            // -------- DEALLOCATE --------
            else if (content.startsWith("DEALLOCATE")) {
                String instanceId = content.split(":")[1];
                VmInfo vm = runningVMs.remove(instanceId);

                if (vm != null) {
                    cloudSimEngine.destroyVm(vm.vmId);
                    releaseResources(vm);
                    sendLoadToManager();
                }

                reply(msg, ACLMessage.INFORM,
                        "DEALLOCATED:INSTANCE=" + instanceId +
                        ",AGENT=" + getLocalName());
            }

            // -------- KILL --------
            else if (content.startsWith("KILL")) {
                String instanceId = content.split(":")[1];
                VmInfo vm = runningVMs.remove(instanceId);

                if (vm != null) {
                    cloudSimEngine.destroyVm(vm.vmId);
                    releaseResources(vm);
                }

                sendLoadToManager();

                reply(msg, ACLMessage.INFORM,
                        "KILLED:INSTANCE=" + instanceId +
                        ",AGENT=" + getLocalName());
            }
        }
    }

    // ==================================================
    // LOAD REPORT (FIXED LOCATION)
    // ==================================================
    private void sendLoadToManager() {

        double avgExec =
                tasksExecuted == 0 ? 0 :
                totalExecutionTime / tasksExecuted;

        ACLMessage load = new ACLMessage(ACLMessage.INFORM);
        load.addReceiver(new AID("ManagerAgent", AID.ISLOCALNAME));
        load.setContent(
                "LOAD:" +
                "AGENT=" + getLocalName() +
                ",USED_CPU=" + usedCPU +
                ",USED_RAM=" + usedRAM +
                ",USED_MIPS=" + usedMIPS +
                ",FREE_CPU=" + (TOTAL_CPU - usedCPU) +
                ",FREE_RAM=" + (TOTAL_RAM - usedRAM) +
                ",FREE_MIPS=" + (TOTAL_MIPS - usedMIPS) +
                ",TASKS=" + tasksExecuted +
                ",AVG_EXEC_TIME=" + avgExec
        );
        send(load);
    }

    private boolean canFit(VmRequest r) {
        return (TOTAL_CPU - usedCPU) >= r.cpu &&
               (TOTAL_RAM - usedRAM) >= r.ram &&
               (TOTAL_MIPS - usedMIPS) >= r.mips;
    }

    private void releaseResources(VmInfo vm) {
        usedCPU -= vm.req.cpu;
        usedRAM -= vm.req.ram;
        usedMIPS -= vm.req.mips;
    }

    private void reply(ACLMessage msg, int perf, String content) {
        ACLMessage r = msg.createReply();
        r.setPerformative(perf);
        r.setContent(content);
        send(r);
    }

    // ==================================================
    // SUPPORT CLASSES
    // ==================================================
    private static class VmInfo {
        String instanceId;
        int vmId;
        VmRequest req;

        VmInfo(String instanceId, int vmId, VmRequest req) {
            this.instanceId = instanceId;
            this.vmId = vmId;
            this.req = req;
        }
    }

    private static class VmRequest {
        int cpu, ram, mips;

        static VmRequest parse(String spec) {
            VmRequest r = new VmRequest();
            String[] parts = spec.split(",");
            r.cpu = Integer.parseInt(parts[0].split("=")[1]);
            r.ram = Integer.parseInt(parts[1].split("=")[1]);
            r.mips = Integer.parseInt(parts[3].split("=")[1]);
            return r;
        }
    }
}
