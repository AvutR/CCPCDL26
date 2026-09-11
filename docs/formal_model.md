# Formal Model

## Objective

The platform chooses which sandboxes to keep warm and which to evict in order to minimize a weighted combination of warm resource cost, cold-start penalties, migration/eviction costs, and SLO violations.

A compact formulation is:

```text
minimize   Σ_t Σ_i [ C_warm · warm_i,t
                    + C_cold · P(miss_i,t)
                    + C_migrate · evict_i,t
                    + C_SLO · 1(SLO_i violated at t) ]

subject to  Σ_i mem_i,t ≤ Capacity_node        (per node, per t)
            warm_i,t ∈ {0,1}
            fairness_i ≥ w_i · min_share        (per tenant weight)
```

## Interpretation

- `warm_i,t` indicates whether tenant `i`'s sandbox is warm at time `t`.
- `P(miss_i,t)` is the probability that a needed sandbox is not warm and therefore incurs cold-start latency.
- `evict_i,t` captures explicit eviction/migration overhead.
- `SLO_i violated at t` penalizes failures to respect latency objectives under the platform's target service level.
- `Capacity_node` is the finite warm-memory budget available to the shared node.

## Fairness constraint

A fairness term can be modeled as a reservation or minimum-share constraint, e.g. each tenant receives a weighted share of the warm pool. The project uses this as a high-level scheduling requirement so that a policy cannot trivially over-prioritize only the largest or most predictable tenants.

## Simplifications for this sprint

- Single shared node abstraction
- No explicit control-plane modeling
- Synthetic traces calibrated to published latency and memory observations
- Discrete-event simulation with probabilistic forecast inputs

These assumptions are deliberate and should be disclosed as modeling simplifications in the paper.
