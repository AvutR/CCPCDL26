import java.util.Map;
import java.util.ArrayList;
import java.util.List;


public class ReactivePolicy implements Policy {

    @Override
    public List<Decision> decide(List<Request> requests,
            SystemState state,
            Map<String, Tool> tools) {

                List<Decision> decisions = new ArrayList<>();

                for(Request request : requests) {

                    String toolId = request.getTrueNextTool();

                    if(!state.isWarm(toolId)) {

                        decisions.add(Decision.warm(toolId));

                    }
                }

            return decisions;
    }
}