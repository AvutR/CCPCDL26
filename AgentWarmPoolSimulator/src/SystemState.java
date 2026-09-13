import java.util.HashMap;
import java.util.Map;

public class SystemState {

    private double totalCapacityGb;
    private Map<String, Tool> warmTools;

    public SystemState(double totalCapacityGb) {
        this.totalCapacityGb = totalCapacityGb;
        this.warmTools = new HashMap<>();
    }

    public boolean isWarm(String toolId) {
        return warmTools.containsKey(toolId);
    }

    public boolean canFit(Tool tool) {
        return getUsedMemoryGb() + tool.getMemFootprint() <= totalCapacityGb;
    }

    public void warm(Tool tool) {
        warmTools.put(tool.getToolId(), tool);
    }

    public void evict(String toolId) {
        warmTools.remove(toolId);
    }

    public double getUsedMemoryGb() {
        double used = 0;

        for(Tool tool : warmTools.values()) {
            used += tool.getMemFootprint();
        }
        return used;
    }

    public double getFreeMemory() {
        return totalCapacityGb - getUsedMemoryGb();
    }

    public Map<String, Tool> getWarmTools() {
        return warmTools;
    }

    public double getTotalCapacityGb() {
        return totalCapacityGb;
    }
}