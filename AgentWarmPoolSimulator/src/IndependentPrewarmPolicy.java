import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndependentPrewarmPolicy implements Policy {
    private final double threshold;

    public IndependentPrewarmPolicy() {
        this.threshold = 0.5; // Default prediction threshold
    }

    public IndependentPrewarmPolicy(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public List<Decision> decide(List<Request> requests, SystemState state, Map<String, Tool> tools) {
        List<Decision> decisions = new ArrayList<>();

        for (Request req : requests) {
            Prediction pred = req.getPrediction();
            boolean tookAction = false;

            if (pred != null && pred.getProbabilities() != null) {
                for (Map.Entry<String, Double> entry : pred.getProbabilities().entrySet()) {
                    String predictedTool = entry.getKey();
                    double prob = entry.getValue();

                    // Prewarm independently if forecasted probability >= threshold
                    if (prob >= threshold && predictedTool != null && !state.isWarm(predictedTool)) {
                        decisions.add(Decision.warm(predictedTool));
                        tookAction = true;
                        break; 
                    }
                }
            }

            // Fallback: check required tool if no prediction prewarmed
            if (!tookAction) {
                String toolNeeded = req.getTrueNextTool();
                if (toolNeeded != null && !state.isWarm(toolNeeded)) {
                    decisions.add(Decision.warm(toolNeeded));
                } else {
                    decisions.add(Decision.none());
                }
            }
        }

        return decisions;
    }
}
