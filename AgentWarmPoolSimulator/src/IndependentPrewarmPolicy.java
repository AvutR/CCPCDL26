import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class IndependentPrewarmPolicy implements Policy {

    @Override
    public List<Decision> decide(List<Request> requests, SystemState state, Map<String, Tool> tools) {

        List<Decision> decisions =
                new ArrayList<>();
        
        for(Request request : requests) {

            String bestTool = null;
            double bestProbability = -1;

            for(Map.Entry<String, Double> entry : request.getPrediction().getProbabilities().entrySet()) {
            
                if(entry.getValue()> bestProbability) {
                    bestProbability = entry.getValue();
                    bestTool = entry.getKey();
                }
            }
        
        
            if(bestTool != null && !state.isWarm(bestTool)) {
                decisions.add(Decision.warm(bestTool));
            }
        }
        return decisions;
    }
}