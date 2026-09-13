public class Tool {
    private String toolId;
    private double memFootprint;
    private double imagePullMs;
    private double runtimeInitMs;
    private double execDurationMs;

    public Tool(String toolId,
                double memFootprint,
                double imagePullMs,
                double runtimeInitMs,
                double execDurationMs) {

        this.toolId = toolId;
        this.memFootprint = memFootprint;
        this.imagePullMs = imagePullMs;
        this.runtimeInitMs = runtimeInitMs;
        this.execDurationMs = execDurationMs;
    }
    public String getToolId() {
        return toolId;
    }

    public double getMemFootprint() {
        return memFootprint;
    }

    public double getImagePullMs() {
        return imagePullMs;
    }

    public double getRuntimeInitMs() {
        return runtimeInitMs;
    }

    public double getExecDurationMs() {
        return execDurationMs;
    }

    public double getColdStartMs() {
        return imagePullMs + runtimeInitMs;
    }
}