public class Decision {
    public enum Action {
        NONE,
        WARM,
        EVICT
    }

    private Action action;
    private String toolId;

    public Decision(Action action, String toolId) {
        this.action = action;
        this.toolId = toolId;
    }

    public static Decision none() {
        return new Decision(Action.NONE, null);
    }

    public static Decision warm(String toolId) {
        return new Decision(Action.WARM, toolId);
    }

    public static Decision evict(String toolId) {
        return new Decision(Action.EVICT, toolId);
    }

    public Action getAction() {
        return action;
    }

    public String getToolId() {
        return toolId;
    }
}