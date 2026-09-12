# Day 1 Sync Notes

## Locked items

- `docs/trace_schema.md` is frozen for the sprint.
- `simulator/policies/base_policy.py` is frozen for the sprint.
- The required metrics table is locked in `docs/metrics.md`.
- The policy design is locked to the greedy marginal-value knapsack described in `docs/policy_design.md`.

## Coordination actions

- Create one issue per owner in the ownership table and assign the corresponding responsibilities.
- Set a daily standup schedule for the remaining sprint days.
- Check the simulator core and trace generator by midday on Day 2; do not wait for the end of the week to discover integration drift.
- Review baselines as they land and verify the interface contract before algorithm details get too deep.

## Policy design summary

For each (tenant, tool) forecast with nonzero probability:

value = w_i × P(need) × cold_start_cost_avoided − holding_cost

At each timestep:

1. Enumerate all nonzero predicted candidate pairs.
2. Compute their marginal value.
3. Sort descending by marginal value.
4. Admit the highest-value candidates into the shared warm pool while capacity remains.
5. If a new candidate exceeds the lowest-value resident, evict the lowest-value current resident and replace it.
6. Keep the warm pool aligned with the formal model objective and capacity constraint.

This is the exact resource-allocation interpretation of the formal objective: maximize expected benefit under a finite node memory budget while preserving weights and fairness.
