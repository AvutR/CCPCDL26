# Formal Model — Forecast-Driven Shared Sandbox Scheduling

## 1. Notation

| Symbol | Meaning | Trace schema field |
|---|---|---|
| i (1 to N) | tenant index | tenant_id |
| t | discrete timestep | timestep |
| k (in K) | tool/environment type | tool_id |
| p_i,k,t | tenant i's predicted probability of needing tool k at t | predicted_dist |
| true_next_tool_i,t | tenant i's true next tool at t (ground truth, scoring only) | true_next_tool |
| w_i | tenant priority/SLA weight | weight_w_i |
| l_i | tenant i's latency-penalty coefficient for a missed prediction | derived (config) |
| m_k | memory footprint of tool k's sandbox | mem_footprint |
| s_k | cold-start delay for tool k (approx image_pull_ms + runtime_init_ms) | image_pull_ms, runtime_init_ms |
| c_k | prewarm cost for tool k | derived from exec_duration_dist |
| eta | unit-conversion constant, prewarm cost to memory units | config |
| C | shared node memory capacity | config |
| R_t | max memory that can be newly prewarmed in one timestep | config |
| x_i,k,t (0 or 1) | is tenant i's sandbox for tool k warm at t | decision variable |
| y_i,k,t (0 or 1) | was a new prewarm triggered for (i,k) at t | decision variable |
| evict_i,k,t | was (i,k) evicted between t-1 and t | derived |

## 2. Objective

The platform minimizes total cost, summed over all timesteps and all tenants:

total_cost = sum over t, sum over i of:
  ( C_warm * warm_i,t )
  + ( C_cold * P(miss_i,t) )
  + ( C_migrate * evict_i,t )
  + ( C_SLO * 1[SLO_i violated at t] )

In plain terms: add up the cost of staying warm, the expected cost of cold starts, the cost of evictions, and a penalty every time a tenant's SLO is violated. The platform's goal is to keep this total as low as possible across the whole run.

Per-tenant expected cost at each timestep, expanded:

L_i,t = sum over k of [ p_i,k,t * ( (1 - x_i,k,t) * cold_cost_k + x_i,k,t * warm_cost ) ]
        + C_migrate * (sum over k of evict_i,k,t)
        + C_SLO * 1[SLO_i violated]

Where:
- cold_cost_k = s_k (the cold-start delay), calibrated as a lognormal distribution per tool, based on the calibration sources.
- warm_cost is a small near-constant cost (approx 25ms resume time), used whenever a sandbox is already warm.

## 3. Constraints

**Shared capacity** — total warm memory across all tenants can never exceed the node's budget, at every timestep:

sum over i,k of ( m_k * x_i,k,t ) <= C, for every t

**Prewarm-trigger bookkeeping** — a prewarm trigger is recorded whenever a sandbox goes from not-warm to warm:

y_i,k,t >= x_i,k,t - x_i,k,t-1, for every i, k, t

**Prewarm rate limit** — prevents the simulator from instantiating unlimited sandboxes in a single step:

sum over i,k of ( m_k * y_i,k,t ) <= R_t, for every t

## 4. SLO indicator

A request violates its SLO if its observed latency exceeds the tenant's threshold:

v_i,t = 1 if L_i,t > tau_i, else 0

Where the observed latency L_i,t is:
- warm_latency_k, if the request was a warm hit
- warm_latency_k + s_k, if the request was a cold start

## 5. Fairness

Define a per-tenant "loss ratio" q_i as:

q_i = (weighted cold-start penalty for tenant i) / (forecast-weighted demand for tenant i + epsilon)

Then fairness is measured as the variance of this ratio across all tenants:

Phi_t = sum over i of (q_i - average_q)^2

Phi_t enters the objective multiplied by a weight, rho. Set rho = 0 for the first working run — get the core policy running and report fairness purely as a metric; enable the fairness penalty only once early numbers look reasonable.

## 6. Arbitration policy

Candidates are ranked by expected value **density** — the value delivered per unit of shared capacity the candidate consumes, not raw value alone:

M_i,k,t = ( w_i * l_i * p_i,k,t * s_k ) / ( m_k + eta * c_k )

Numerator = how much good comes from warming this sandbox (priority-weighted, probability-weighted, cold-start-severity-weighted).
Denominator = how much shared memory/cost this sandbox consumes.

Dividing by resource cost is the standard greedy rule for capacity-constrained allocation: it prevents one large, memory-hungry sandbox from being admitted over several smaller ones that would jointly deliver more value in the same shared space.

**Policy steps:**
1. At each timestep, collect all nonzero p_i,k,t for every (tenant, tool) pair, computed globally across all tenants before any admission decision is made.
2. Compute M_i,k,t for every candidate.
3. Sort all candidates by descending M_i,k,t.
4. Greedily admit candidates into the shared pool while capacity C remains.
5. If a new candidate's M exceeds the lowest-M current resident, evict that resident and admit the candidate instead.
6. A request counts as a warm hit when the tenant's true next tool is already in the warm pool that timestep.

**Known limitation:** greedy-by-density is not provably optimal for indivisible (0/1) capacity allocation — it can occasionally pick one high-density item that consumes the entire remaining budget, instead of several lower-density items that would jointly fit and sum to more total value. This is an accepted limitation for a simulator-scale study; note it alongside "no exact solver" and "single-node capacity" in the paper's limitations section.

## 7. Differentiation

This work studies allocation of a finite shared sandbox budget using tenant-specific probabilistic next-tool forecasts, under cold-start, warm-resource, SLO, and fairness costs. This differs from prior work in three ways:

- **Single-agent, single-host prediction/prewarming systems** — those optimize one agent's own runtime with no cross-tenant capacity competition.
- **Multi-tenant cache-replacement systems** — those share containers across tenants using backward-looking access recency/frequency, not forward-looking, forecast-weighted value density.
- **Static, interval-based pool-sizing methods** — those show that even a perfect predictor fails to meet a target SLO when pool size is recomputed periodically rather than decided per-candidate against a live shared budget; this motivates a per-timestep, per-candidate decision rule instead.

## 8. Open items

- Confirm eta (cost-to-memory conversion constant) is consistent with the calibrated cost model in the simulator core.
- Confirm R_t (prewarm rate limit) doesn't starve burst scenarios in the trace generator.
