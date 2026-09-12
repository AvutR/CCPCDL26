# Greedy Marginal-Value Knapsack Policy

## Objective-mapped formulation

For each tenant i and tool k at timestep t, define the candidate warm value as:

value_{i,k,t} = w_i × P_i,k,t(need) × cold_start_cost_avoided_{i,k,t} − holding_cost_{i,k,t}

where:

- w_i is the tenant weight used for fairness
- P_i,k,t(need) is the tenant forecast probability that tool k is needed next
- cold_start_cost_avoided_{i,k,t} is the latency or cost avoided by keeping the sandbox warm instead of cold starting it
- holding_cost_{i,k,t} is the marginal cost of occupying warm memory budget for that candidate

The platform chooses a subset of candidates to keep warm under the shared-capacity constraint:

Σ_i mem_{i,t} ≤ Capacity_node

This is exactly the same pattern as the formal objective in the project model: the policy chooses the warm set that maximizes expected benefit under a scarce resource budget.

## Full pseudocode

```text
Input:
  T = current timestep
  Candidates = {(tenant_id, tool_id): probability, memory, weight}
  WarmPool = current resident set with resident value and memory
  Capacity_node = total warm memory budget

for each timestep t:
    candidate_list = []

    for each tenant i:
        for each tool k in predicted_dist[i]:
            if P_i,k,t(need) > 0:
                cold_start_cost_avoided = C_cold × expected_miss_penalty(i, k, t)
                holding_cost = α × mem_{i,k} + β × expected_residency_cost(i, k, t)
                value = w_i × P_i,k,t(need) × cold_start_cost_avoided − holding_cost
                candidate_list.append({tenant=i, tool=k, value=value, mem=mem_{i,k}})

    sort candidate_list by value descending

    for candidate in candidate_list:
        if candidate is already warm resident:
            update its resident value to candidate.value
            continue

        if memory_used + candidate.mem ≤ Capacity_node:
            admit candidate into WarmPool
            memory_used += candidate.mem
            continue

        lowest_resident = argmin_{resident in WarmPool} resident.value

        if candidate.value > lowest_resident.value:
            evict lowest_resident
            admit candidate into WarmPool
            memory_used = memory_used − lowest_resident.mem + candidate.mem
        else:
            reject candidate

    for each resident in WarmPool:
        if resident has not been refreshed and is no longer competitive:
            evict resident if needed to maintain fairness or keep the best-value set

    return WarmPool
```

## Operational interpretation

1. Every prediction becomes a candidate warm-sandbox placement.
2. The platform ranks all possible placements globally instead of per tenant.
3. Warm memory is treated as a finite shared knapsack.
4. Eviction is not arbitrary; it is driven by the lowest marginal value under the same objective.
5. The heuristic is therefore not a disconnected trick — it is a direct greedily sorted approximation of the constrained objective.

## Why this is defensible in the paper

This policy directly implements the formal model's core tradeoff:

- The objective penalizes cold starts and SLO misses.
- The constraint caps warm memory on the node.
- The algorithm chooses the warm set that maximizes expected objective gain per unit memory while preserving fairness via tenant weights.

That is the strongest story for the methodology section: the algorithm is the platform-level resource allocation policy induced by the formal objective, not a separate ad hoc heuristic.
