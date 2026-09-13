import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReactivePolicy implements Policy {

    @Override
    public List<Decision> decide(List<Request> requests, SystemState state, Map<String, Tool> tools) {
        List<Decision> decisions = new ArrayList<>();

        for (Request req : requests) {
            String toolNeeded = null;

            // Extract the tool with the highest probability from runtime predictions (Non-Oracle)
            if (req.getPrediction() != null && req.getPrediction().getProbabilities() != null) {
                Map<String, Double> probs = req.getPrediction().getProbabilities();
                double maxProb = -1.0;

                for (Map.Entry<String, Double> entry : probs.entrySet()) {
                    if (entry.getValue() > maxProb) {
                        maxProb = entry.getValue();
                        toolNeeded = entry.getKey();
                    }
                }
            }

            // Provision on-demand if the predicted tool is not currently warm
            if (toolNeeded != null && !state.isWarm(toolNeeded)) {
                decisions.add(Decision.warm(toolNeeded));
            } else {
                decisions.add(Decision.none());
            }
        }

        return decisions;
    }
}
