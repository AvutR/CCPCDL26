import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProposedPolicy implements Policy {

    // Unit-conversion constant: converts residency/prewarm cost (ms) into
    // memory-comparable units, matching eta in the formal model (docs/formal_model.md, section 6).
    private final double eta;

    public ProposedPolicy() {
        this.eta = 0.001; // default: keeps residency-cost term small relative to memFootprint (GB)
    }

    public ProposedPolicy(double eta) {
        this.eta = eta;
    }

    @Override
    public List<Decision> decide(
            List<Request> requests,
            SystemState state,
            Map<String, Tool> tools) {

        List<Decision> decisions = new ArrayList<>();

        /*
         * --------------------------------------------------------
         * STEP 1: Aggregate demand per tool across all tenants.
         *
         * Warmth is tracked globally per toolId (see SystemState),
         * so multiple tenants predicting the same tool share one
         * warm instance. We therefore sum weighted demand
         * (weight * probability) across all tenants requesting
         * the same tool, matching the sum-over-i structure of the
         * formal objective (docs/formal_model.md, section 2).
         * --------------------------------------------------------
         */

        Map<String, Double> weightedDemand = new HashMap<>();

        for (Request req : requests) {
            Prediction pred = req.getPrediction();

            if (pred == null || pred.getProbabilities() == null) {
                continue;
            }

            for (Map.Entry<String, Double> entry : pred.getProbabilities().entrySet()) {
                String toolId = entry.getKey();
                double prob = entry.getValue();

                if (prob <= 0 || toolId == null) {
                    continue;
                }

                double contribution = req.getWeight() * prob;

                weightedDemand.merge(toolId, contribution, Double::sum);
            }
        }

        /*
         * --------------------------------------------------------
         * STEP 2: Compute value density M_k for every candidate tool
         * with nonzero aggregated demand.
         *
         * M_k = (weightedDemand_k * coldStartMs_k)
         *       / (memFootprint_k + eta * execDurationMs_k)
         *
         * Numerator: expected benefit of keeping this tool warm,
         * weighted by tenant priority and forecast probability.
         * Denominator: shared capacity this tool consumes.
         * Ranking by density (not raw value) is the correct greedy
         * rule for a capacity-constrained admission problem — see
         * docs/formal_model.md, section 6, for the full rationale.
         * --------------------------------------------------------
         */

        List<Candidate> candidates = new ArrayList<>();

        for (Map.Entry<String, Double> entry : weightedDemand.entrySet()) {
            String toolId = entry.getKey();
            double demand = entry.getValue();

            Tool tool = tools.get(toolId);

            if (tool == null) {
                continue;
            }

            double numerator = demand * tool.getColdStartMs();
            double denominator = tool.getMemFootprint() + eta * tool.getExecDurationMs();

            double value = denominator > 0 ? numerator / denominator : 0;

            candidates.add(new Candidate(toolId, value, tool));
        }

        // Sort candidates by descending value density.
        candidates.sort((a, b) -> Double.compare(b.value, a.value));

        /*
         * --------------------------------------------------------
         * STEP 3: Greedily admit candidates under the shared
         * capacity budget, evicting the lowest-value current
         * resident when a higher-value candidate arrives and
         * there isn't room otherwise.
         *
         * Tools currently warm but not requested this timestep are
         * treated as value 0 (prime eviction candidates). Tools
         * already warm AND still in demand this timestep are
         * skipped (no action needed) and protected from eviction
         * this round, since they are actively being used.
         * --------------------------------------------------------
         */

        double freeMemory = state.getFreeMemory();

        // Track tools we've protected (already warm and in demand this
        // round) so they are never picked as eviction targets below.
        Map<String, Boolean> protectedThisRound = new HashMap<>();

        for (Candidate candidate : candidates) {

            if (state.isWarm(candidate.toolId)) {
                // Already warm and still wanted — nothing to do, but
                // protect it from eviction consideration this round.
                protectedThisRound.put(candidate.toolId, true);
                continue;
            }

            if (candidate.tool.getMemFootprint() <= freeMemory) {
                decisions.add(Decision.warm(candidate.toolId));
                freeMemory -= candidate.tool.getMemFootprint();
                continue;
            }

            // Not enough room — look for the lowest-value current
            // resident to evict in its place.
            String lowestResidentId = null;
            double lowestResidentValue = Double.POSITIVE_INFINITY;

            for (String warmToolId : state.getWarmTools().keySet()) {

                if (protectedThisRound.containsKey(warmToolId)) {
                    continue; // in active demand this round, do not evict
                }

                // A resident's value this round is its aggregated demand
                // if any tenant still wants it, otherwise 0.
                double residentValue = weightedDemand.getOrDefault(warmToolId, 0.0);

                if (residentValue < lowestResidentValue) {
                    lowestResidentValue = residentValue;
                    lowestResidentId = warmToolId;
                }
            }

            if (lowestResidentId != null && candidate.value > lowestResidentValue) {
                Tool evictedTool = tools.get(lowestResidentId);

                decisions.add(Decision.evict(lowestResidentId));
                decisions.add(Decision.warm(candidate.toolId));

                if (evictedTool != null) {
                    freeMemory += evictedTool.getMemFootprint();
                    freeMemory -= candidate.tool.getMemFootprint();
                }

                // The evicted tool can no longer be evicted again this round.
                protectedThisRound.put(lowestResidentId, true);
            }
            // else: candidate rejected, not enough value to displace anything.
        }

        return decisions;
    }

    /**
     * Internal helper — one candidate tool with its aggregated value density.
     */
    private static class Candidate {
        final String toolId;
        final double value;
        final Tool tool;

        Candidate(String toolId, double value, Tool tool) {
            this.toolId = toolId;
            this.value = value;
            this.tool = tool;
        }
    }
}
