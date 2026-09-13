public class Request {

    private String tenantId;
    private int timestep;
    private String trueNextTool;
    private Prediction prediction;
    private double weight;

    public Request(String tenantId, int timestep, String trueNextTool, Prediction prediction, double weight) {
        this.tenantId = tenantId;
        this.timestep = timestep;
        this.trueNextTool = trueNextTool;
        this.prediction = prediction;
        this.weight = weight;
    }

    public String getTenantId() {
        return tenantId;
    }

    public int getTimestep() {
        return timestep;
    }

    public String getTrueNextTool() {
        return trueNextTool;
    }

    public Prediction getPrediction() {
        return prediction;
    }

    public double getWeight() {
        return weight;
    }
}