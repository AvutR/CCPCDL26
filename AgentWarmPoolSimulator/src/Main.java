import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Main {

    public static void main(String[] args)
            throws Exception {

        /*
         * --------------------------------
         * 1. Input files
         * --------------------------------
         */

        String traceFile =
                "trace_generator/sample_trace.csv";

        String toolsFile =
                "trace_generator/tools.csv";


        /*
         * --------------------------------
         * 2. Load trace
         * --------------------------------
         */

        List<Request> requests =
                TraceLoader.loadTrace(
                        traceFile
                );


        /*
         * --------------------------------
         * 3. Load tools
         * --------------------------------
         */

        Map<String, Tool> tools =
                ToolLoader.loadTools(
                        toolsFile
                );

        /*
         * --------------------------------
         * 4. Select policy
         * --------------------------------
         */

        String policyName = args.length > 0 ? args[0] : "reactive";

        runPolicy(policyName, requests, tools);
        
    }
    
    public static void runPolicy(String policyName,
        List<Request> requests,
        Map<String, Tool> tools) throws Exception {
            
            Policy policy;

            switch(policyName) {
                case "reactive":
                    policy = new ReactivePolicy();
                    break;
                case "independent":
                    policy = new IndependentPrewarmPolicy();
                    break;
                case "oracle":
                    policy = new OraclePolicy();
                    break;
                case "proposed":
                    policy = new ProposedPolicy();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown policy: " + policyName);
            }

            /*
            * --------------------------------
            * 4. Create simulator
            * --------------------------------
            */

            Simulator simulator =
                    new Simulator(
                            8.0,       // 8 GB pool
                            policy,
                            tools,
                            2000,       // SLO = 2 sec
                            policyName
                    );

            /*
            * --------------------------------
            * 5. Run
            * --------------------------------
            */

            simulator.run(requests);
    }
}