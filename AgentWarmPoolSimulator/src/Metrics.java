import java.util.ArrayList;
import java.util.List;

public class Metrics {

    private List<Double> latencies = new ArrayList<>();

    private int totalRequests = 0;
    private int coldStarts = 0;
    private int warmHits = 0;
    private int sloViolations = 0;

    private double totalMemoryHours = 0;

    public void recordRequest(
            double latency,
            boolean coldStart,
            boolean warmHit) {

        totalRequests++;
        latencies.add(latency);

        if (coldStart) {
            coldStarts++;
        }

        if (warmHit) {
            warmHits++;
        }
    }

    public void recordMemoryHours(double memoryHours) {
        totalMemoryHours += memoryHours;
    }

    public void checkSLO(double latency, double sloMs) {
        if (latency > sloMs) {
            sloViolations++;
        }
    }

    public void printSummary(String policyName) {

    System.out.println();
    System.out.println(
            "========== " + policyName.toUpperCase()
            + " RESULTS =========="
    );

    System.out.println(
            "Total requests   : " + totalRequests
    );

    System.out.println(
            "Cold starts      : " + coldStarts
    );

    System.out.println(
            "Warm hits        : " + warmHits
    );

    if (totalRequests > 0) {

        System.out.println(
                "Cold-start rate  : "
                + ((double) coldStarts / totalRequests)
        );
    }

    System.out.println(
            "Memory-hours     : " + totalMemoryHours
    );

    System.out.println(
            "SLO violations   : " + sloViolations
    );

    System.out.println(
            "P50 latency      : " + percentile(50)
    );

    System.out.println(
            "P95 latency      : " + percentile(95)
    );

    System.out.println(
            "P99 latency      : " + percentile(99)
    );

    System.out.println(
            "============================="
    );
}

    private double percentile(double percentile) {

        if (latencies.isEmpty()) {
            return 0;
        }

        List<Double> sorted = new ArrayList<>(latencies);
        sorted.sort(Double::compareTo);

        double index =
                (percentile / 100.0) * (sorted.size() - 1);

        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double fraction = index - lower;

        return sorted.get(lower)
                + fraction * (sorted.get(upper)
                - sorted.get(lower));
    }
}