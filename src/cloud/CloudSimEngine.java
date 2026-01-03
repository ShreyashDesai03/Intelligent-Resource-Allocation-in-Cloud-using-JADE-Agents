package cloud;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.*;

/**
 * CloudSimEngine simulates execution of ONE VM workload
 * and returns execution metrics.
 *
 * CloudSim is re-initialized per request to mimic
 * EC2-style isolated VM execution.
 */
public class CloudSimEngine {

    /* ==================================================
       RESULT OBJECT (USED BY ResourceAgent)
       ================================================== */
    public static class TaskResult {
        public int vmId;
        public double startTime;
        public double finishTime;
        public double executionTime;

        @Override
        public String toString() {
            return "VM=" + vmId +
                   " | Start=" + startTime +
                   " | Finish=" + finishTime +
                   " | ExecTime=" + executionTime;
        }
    }

    // Local VM id generator (safe even across runs)
    private static int VM_ID_SEQ = 0;

    /**
     * Creates ONE VM, runs ONE cloudlet,
     * and returns execution metrics.
     *
     * @param taskData Format:
     * CPU=2,RAM=2048,DISK=10000,MIPS=2000
     */
    public TaskResult createAndRunVm(String taskData) throws Exception {

        /* ==================================================
           1️⃣ INITIALIZE CLOUDSIM
           ================================================== */
        CloudSim.init(1, Calendar.getInstance(), false);

        /* ==================================================
           2️⃣ CREATE HOST (PHYSICAL MACHINE)
           ================================================== */
        int hostCpu = 8;
        int hostRam = 16000;
        long hostStorage = 200000;
        int hostMips = 25000;
        long hostBw = 10000;

        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < hostCpu; i++) {
            peList.add(new Pe(i,
                    new PeProvisionerSimple(hostMips)));
        }

        Host host = new Host(
                0,
                new RamProvisionerSimple(hostRam),
                new BwProvisionerSimple(hostBw),
                hostStorage,
                peList,
                new VmSchedulerSpaceShared(peList)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        "x86",
                        "Linux",
                        "Xen",
                        hostList,
                        10.0,
                        3.0,
                        0.05,
                        0.1,
                        0.1
                );

        new Datacenter(
                "Datacenter_1",
                characteristics,
                new VmAllocationPolicySimple(hostList),
                new LinkedList<>(),
                0
        );

        /* ==================================================
           3️⃣ CREATE BROKER
           ================================================== */
        DatacenterBroker broker =
                new DatacenterBroker("Broker");

        /* ==================================================
           4️⃣ PARSE TASK SPEC
           ================================================== */
        String[] parts = taskData.split(",");

        int cpu = Integer.parseInt(parts[0].split("=")[1]);
        int ram = Integer.parseInt(parts[1].split("=")[1]);
        long disk = Long.parseLong(parts[2].split("=")[1]);
        int mips = Integer.parseInt(parts[3].split("=")[1]);

        /* ==================================================
           5️⃣ CREATE VM
           ================================================== */
        int vmId = VM_ID_SEQ++;

        Vm vm = new Vm(
                vmId,
                broker.getId(),
                mips,
                cpu,
                ram,
                hostBw,
                disk,
                "Xen",
                new CloudletSchedulerSpaceShared()
        );

        broker.submitVmList(
                Collections.singletonList(vm));

        /* ==================================================
           6️⃣ CREATE CLOUDLET
           ================================================== */
        long length = mips * 10L;        // workload size
        long fileSize = 300;             // bytes
        long outputSize = 300;           // bytes

        UtilizationModel um =
                new UtilizationModelFull();

        Cloudlet cloudlet = new Cloudlet(
                0,
                length,
                cpu,
                fileSize,
                outputSize,
                um, um, um
        );

        cloudlet.setUserId(broker.getId());
        cloudlet.setVmId(vmId);

        broker.submitCloudletList(
                Collections.singletonList(cloudlet));

        /* ==================================================
           7️⃣ RUN SIMULATION
           ================================================== */
        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        List<Cloudlet> finishedList =
                broker.getCloudletReceivedList();

        if (finishedList.isEmpty()) {
            throw new RuntimeException(
                    "CloudSim execution failed: no cloudlet finished");
        }

        Cloudlet finished = finishedList.get(0);

        /* ==================================================
           8️⃣ BUILD RESULT
           ================================================== */
        TaskResult result = new TaskResult();
        result.vmId = vmId;
        result.startTime = finished.getExecStartTime();
        result.finishTime = finished.getFinishTime();
        result.executionTime =
                finished.getActualCPUTime();

        System.out.println("CloudSim → " + result);

        return result;
    }

    /**
     * Logical destroy (CloudSim already stopped).
     * Kept only for EC2-like lifecycle semantics.
     */
    public void destroyVm(int vmId) {
        System.out.println(
                "CloudSim → VM " + vmId + " logically destroyed");
    }
}
