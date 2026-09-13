import java.util.Map;

public class Prediction {

    private Map<String, Double> probabilities;
    
    public Prediction(Map<String, Double> probabilities) {
        this.probabilities = probabilities;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }
}