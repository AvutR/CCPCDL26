# Ownership Issue Drafts

These issue drafts are ready to paste into GitHub once authentication is available.

## Akhil — Proposed policy + coordination

Title: Implement greedy marginal-value cross-tenant scheduling policy

Body:
- Implement the proposed warm-pool arbitration policy under the shared policy interface.
- Use the objective-mapped score: w_i × P(need) × cold_start_cost_avoided − holding_cost.
- Rank all (tenant, tool) pairs globally each timestep.
- Greedily admit into the warm pool until capacity is hit.
- Evict the lowest-value resident when a higher-value candidate arrives.
- Validate with a smoke test on 1 seed / 1 short trace before broader evaluation.
- Write the methodology section prose connecting this policy to the formal objective.

## Adersh — Formal objective + Oracle baseline

Title: Formalize the objective and verify the Oracle baseline against it

Body:
- Confirm the objective, constraints, and fairness term match the project model.
- Ensure the Oracle baseline corresponds to perfect next-tool knowledge under the same cost model.
- Check that the implementation matches the math in the methodology section.
- Review the policy-to-objective mapping for reviewer defense.

## Tarkesh — Simulator core + cost model

Title: Build the discrete-event simulator and calibrated cost model

Body:
- Implement the shared-node discrete-event engine.
- Add the calibrated cost model for cold start, warm resume, and memory holding costs.
- Ensure the policy interface is respected across all schedulers.
- Validate a no-op / reactive smoke test before broader comparisons.

## Shalini — Reactive and independent prewarm baselines

Title: Implement reactive and independent prewarm baselines

Body:
- Implement the reactive baseline with no predictive component.
- Implement the independent per-tenant prewarming baseline using each tenant's own forecast.
- Ensure both baselines match the locked policy interface and the same metric schema.
- Run a 1-seed smoke test to ensure plausible cold-start behavior.

## Shloka — Trace generator and burst knobs

Title: Implement calibrated synthetic trace generator with burst and accuracy knobs

Body:
- Implement the synthetic multi-tenant workload generator.
- Expose forecast-accuracy and burst-correlation tuning knobs.
- Make sure the output matches the locked trace schema.
- Produce a short, reproducible trace for simulator smoke tests.

## Jeryl — FaaSCamp-style baseline and robustness experiments

Title: Implement the FaaSCamp-style cross-tenant baseline and robustness checks

Body:
- Implement a shared caching baseline that reflects FaaSCamp-style cross-tenant reuse.
- Compare it against the oracle and proposed policy under the same trace streams.
- Run robustness checks for burst correlation and forecast inaccuracy.
- Report performance under the locked metrics table.
