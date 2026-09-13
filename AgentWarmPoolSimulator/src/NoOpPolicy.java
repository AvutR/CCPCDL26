import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class NoOpPolicy implements Policy {

    @Override
    public List<Decision> decide(
            List<Request> requests,
            SystemState state,
            Map<String, Tool> tools) {

        return new ArrayList<>();
    }
}