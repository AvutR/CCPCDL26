import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OraclePolicy implements Policy {

    @Override
    public List<Decision> decide(
            List<Request> requests,
            SystemState state,
            Map<String, Tool> tools) {

        List<Decision> decisions = new ArrayList<>();

        for (Request request : requests) {

            String trueTool = request.getTrueNextTool();

            if (trueTool == null || !tools.containsKey(trueTool)) {
                continue;
            }

            if (!state.isWarm(trueTool)) {
                decisions.add(Decision.warm(trueTool));
            }
        }

        return decisions;
    }
}
