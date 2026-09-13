import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReactivePolicy implements Policy {

    @Override
    public List<Decision> decide(List<Request> requests, SystemState state, Map<String, Tool> tools) {
        List<Decision> decisions = new ArrayList<>();

        for (Request req : requests) {
            String toolNeeded = req.getTrueNextTool();

            // Provision on-demand if the required tool is not currently warm
            if (toolNeeded != null && !state.isWarm(toolNeeded)) {
                decisions.add(Decision.warm(toolNeeded));
            } else {
                decisions.add(Decision.none());
            }
        }

        return decisions;
    }
}
