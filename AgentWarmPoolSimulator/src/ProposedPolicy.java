import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProposedPolicy implements Policy {

    @Override
    public List<Decision> decide(
            List<Request> requests,
            SystemState state,
            Map<String, Tool> tools) {

        // List<Decision> decisions = new ArrayList<>();

        /*
         * Temporary placeholder.
         *
         * Akhil + Adersh will provide
         * the actual cross-tenant policy.
         */

        
        //return decisions;
        return new ArrayList<>();
    }
}