import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Simulator {

    private SystemState state;
    private Policy policy;
    private Map<String , Tool> tools;
    private Metrics metrics;
    

    private double sloMs;
    private String policyName;

    public Simulator(double capacityGb, Policy policy, Map<String, Tool> tools,
    double sloMs, String policyName) {

        this.state = new SystemState(capacityGb);
        this.policy = policy;
        this.tools = tools;
        this.metrics = new Metrics();
        this.sloMs = sloMs;
        this.policyName = policyName;
    }

    public void run(List<Request> requests) {
        /*
         * --------------------------------
         * GROUP REQUESTS BY TIMESTEP
         * --------------------------------
         */

        Map<Integer, List<Request>> requestsByTimestep = new TreeMap<>();

        for(Request request : requests) {

            requestsByTimestep
                    .computeIfAbsent(request.getTimestep(), k -> new ArrayList<>())
                    .add(request);

        }


        /*
         * --------------------------------
         * PROCESS ONE TIMESTEP AT A TIME
         * --------------------------------
         */

        for(Map.Entry<Integer, List<Request>> entry : requestsByTimestep.entrySet()) {

            int timestep = entry.getKey();

            List<Request> timestepRequests = entry.getValue();

            System.out.println();
            System.out.println(
                    "================================"
            );

            System.out.println(
                    "TIMESTEP: " + timestep
            );

            System.out.println(
                    "TENANTS: "
                    + timestepRequests.size()
            );

            System.out.println(
                    "================================"
            );
            
            /*
             * --------------------------------
             * SHOW REQUESTS IN THIS TIMESTEP
             * --------------------------------
             */

            for (Request request :
                    timestepRequests) {

                System.out.println(
                        "Tenant "
                        + request.getTenantId()
                        + " -> "
                        + request.getTrueNextTool()
                );
            }

            // Ask policy what to do.
            List<Decision> decisions =
                    policy.decide(
                            timestepRequests,
                            state,
                            tools
                    );

            // Apply policy decision.
            applyDecisions(decisions);

            // Execute request.
            for (Request request :
                    timestepRequests) {

                executeRequest(request);
            }

            metrics.recordMemoryHours(state.getUsedMemoryGb());


            System.out.println(
                    "Used memory after timestep: "
                    + state.getUsedMemoryGb()
                    + " GB"
            );

        }

        metrics.printSummary(policyName);

    }

    private void applyDecisions(List<Decision> decisions) {

        for(Decision decision : decisions) {

            if(decision.getAction() == Decision.Action.WARM) {
                
                String toolId = decision.getToolId();

                Tool tool = tools.get(toolId);

                if(tool == null) {

                    System.out.println("Warning: unknown tool" + toolId);

                    continue;
                }

                /*
                 * Already warm?
                 */

                if (state.isWarm(toolId)) {

                    System.out.println(
                            "Already warm: "
                            + toolId
                    );

                    continue;
                }

                if(state.canFit(tool)) {

                    state.warm(tool);

                    System.out.println(
                            "WARMED: "
                            + toolId
                            + " | Memory: "
                            + tool.getMemFootprint()
                            + " GB"
                    );

                }
                else {

                    System.out.println(
                            "CANNOT WARM: "
                            + toolId
                            + " | Required: "
                            + tool.getMemFootprint()
                            + " GB"
                            + " | Free: "
                            + state.getFreeMemory()
                            + " GB"
                    );
                }
            }
        

            else if(decision.getAction() == Decision.Action.EVICT) {
                
                String toolId = decision.getToolId();

                if(state.isWarm(toolId)) {

                    state.evict(decision.getToolId());

                    System.out.println("EVICTED: " + toolId);

                }

            }
        }
    }

    private void executeRequest(Request request) {
        
        String toolId = request.getTrueNextTool();

        Tool tool = tools.get(toolId);

        // unknown tool

        if(tool == null) {
            
            System.out.println("ERROR: Unknown tool " + toolId);

            return;
        }

        // check if warm
        boolean hit = state.isWarm(toolId);

        double latency;

        if(hit) {
            
            latency = tool.getExecDurationMs();

            System.out.println(
                                "Tenant "
                                + request.getTenantId()
                                + " | "
                                + toolId
                                + " | WARM HIT"
                                + " | Latency: "
                                + latency
                                + " ms"
                        );  
        }

        else {

            latency = tool.getColdStartMs() + tool.getExecDurationMs();

            System.out.println(
                    "Tenant "
                    + request.getTenantId()
                    + " | "
                    + toolId
                    + " | COLD START"
                    + " | Latency: "
                    + latency
                    + " ms"
            );
        } 


        /*
         * Record metrics
         */

        metrics.recordRequest(
                latency,
                !hit,
                hit
        );

        metrics.checkSLO(
                latency,
                sloMs
        );
    }
}