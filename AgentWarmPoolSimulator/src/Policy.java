import java.util.List;
import java.util.Map;
public interface Policy {

    List<Decision> decide(
            List<Request> requests,
            SystemState state,
            Map<String, Tool> tools
    );
}