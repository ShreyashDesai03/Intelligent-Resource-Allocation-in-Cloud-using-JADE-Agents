package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.UUID;

public class ClientAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("ClientAgent started.");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage msg = receive();

                if (msg == null) {
                    block();
                    return;
                }

                String originalContent = msg.getContent();

                // --------------------------------------------------
                // METRICS TAGGING
                // --------------------------------------------------
                String taskId =
                        "task-" + UUID.randomUUID()
                                .toString().substring(0, 8);
                long submitTime = System.currentTimeMillis();

                /*
                 * Final forwarded format example:
                 * ALLOCATE:CPU=2,RAM=25,DISK=2,MIPS=2000
                 * |TASK_ID=task-ab12cd34|SUBMIT_TIME=1735223452345
                 */
                String enhancedContent =
                        originalContent +
                        "|TASK_ID=" + taskId +
                        "|SUBMIT_TIME=" + submitTime;

                System.out.println(
                        "🟡 ClientAgent received: " + originalContent);
                System.out.println(
                        "📎 Tagged → " + taskId +
                        " @ " + submitTime);

                // --------------------------------------------------
                // FORWARD TO ManagerAgent
                // --------------------------------------------------
                ACLMessage forward =
                        new ACLMessage(ACLMessage.REQUEST);
                forward.addReceiver(
                        new AID("ManagerAgent",
                                AID.ISLOCALNAME));
                forward.setContent(enhancedContent);
                send(forward);

                System.out.println(
                        "➡ Forwarded to ManagerAgent");
            }
        });
    }
}
