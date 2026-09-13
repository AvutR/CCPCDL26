# Formal Model — Forecast-Driven Shared Sandbox Scheduling

## 1. Notation

| Symbol | Meaning | Trace schema field |
|---|---|---|
| $i \in \{1,\dots,N\}$ | tenant index | `tenant_id` |
| $t$ | discrete timestep | `timestep` |
| $k \in \mathcal{K}$ | tool/environment type | `tool_id` |
| $p_{i,k,t}$ | tenant $i$'s predicted probability of needing tool $k$ at $t$ | `predicted_dist` |
| $y^{\text{true}}_{i,t}$ | tenant $i$'s true next tool at $t$ (ground truth, scoring only) | `true_next_tool` |
| $w_i$ | tenant priority/SLA weight | `weight_w_i` |
| $\ell_i$ | tenant $i$'s latency-penalty coefficient for a missed prediction | derived (config) |
| $m_k$ | memory footprint of tool $k$'s sandbox | `mem_footprint` |
| $s_k$ | cold-start delay for tool $k$ (≈ `image_pull_ms` + `runtime_init_ms`) | `image_pull_ms`, `runtime_init_ms` |
| $c_k$ | prewarm cost for tool $k$ | derived from `exec_duration_dist` |
| $\eta$ | unit-conversion constant, prewarm cost → memory units | config |
| $C$ | shared node memory capacity | config |
| $R_t$ | max memory that can be newly prewarmed in one timestep | config |
| $x_{i,k,t}\in\{0,1\}$ | is tenant $i$'s sandbox for tool $k$ warm at $t$ | decision variable |
| $y_{i,k,t}\in\{0,1\}$ | was a new prewarm triggered for $(i,k)$ at $t$ | decision variable |
| $\text{evict}_{i,k,t}$ | was $(i,k)$ evicted between $t-1$ and $t$ | derived |

## 2. Objective

$$
\min_{x,y} \; \sum_{t}\sum_{i=1}^{N} \Big[ C_{\text{warm}}\cdot \text{warm}_{i,t} + C_{\text{cold}}\cdot P(\text{miss}_{i,t}) + C_{\text{migrate}}\cdot \text{evict}_{i,t} + C_{\text{SLO}}\cdot \mathbb{1}(\text{SLO}_i \text{ violated at } t)\Big]
$$

Per-tenant expected cost, expanded:
$$
L_{i,t} = \sum_{k} p_{i,k,t}\Big[(1-x_{i,k,t})\, c^{\text{cold}}_k + x_{i,k,t}\, c^{\text{warm}}\Big] + C_{\text{migrate}}\sum_k \text{evict}_{i,k,t} + C_{\text{SLO}}\cdot \mathbb{1}[\text{SLO}_i\text{ violated}]
$$
with $c^{\text{cold}}_k = s_k$ (calibrated lognormal per tool, per the calibration sources) and $c^{\text{warm}}$ a small near-constant (≈25ms resume).

## 3. Constraints

**Shared capacity:**
$$
\sum_{i,k} m_k\, x_{i,k,t} \;\le\; C \qquad \forall t
$$

**Prewarm-trigger bookkeeping:**
$$
y_{i,k,t} \;\ge\; x_{i,k,t} - x_{i,k,t-1} \qquad \forall i,k,t
$$

**Prewarm rate limit** (prevents instantiating unlimited sandboxes in one step):
$$
\sum_{i,k} m_k\, y_{i,k,t} \;\le\; R_t \qquad \forall t
$$

## 4. SLO indicator

$$
v_{i,t} = \mathbb{1}\big[L_{i,t} > \tau_i\big], \qquad
L_{i,t} = \begin{cases} L_k^{\text{warm}} & \text{if warm hit} \\ L_k^{\text{warm}} + s_k & \text{if cold} \end{cases}
$$

## 5. Fairness

$$
q_i = \frac{\text{weighted cold-start penalty for tenant } i}{\text{forecast-weighted demand for tenant } i + \epsilon}, \qquad
\Phi_t = \sum_i (q_i - \bar q)^2
$$

$\Phi_t$ enters the objective with weight $\rho$. Set $\rho = 0$ for the first working run — get the core policy running and report fairness purely as a metric; enable the penalty once early numbers look reasonable.

## 6. Arbitration policy

Candidates are ranked by expected value **density** — value per unit of shared capacity consumed:
$$
M_{i,k,t} = \frac{w_i\, \ell_i\, p_{i,k,t}\, s_k}{m_k + \eta\, c_k}
$$

Dividing by resource cost is the standard greedy rule for capacity-constrained allocation: it prevents one large, memory-hungry sandbox from being admitted over several smaller ones that would jointly deliver more value in the same space.

**Policy steps:**
1. At each timestep, collect all nonzero $p_{i,k,t}$ for every (tenant, tool) pair, computed globally across all tenants before any admission decision.
2. Compute $M_{i,k,t}$ for every candidate.
3. Sort all candidates by descending $M_{i,k,t}$.
4. Greedily admit candidates into the shared pool while capacity $C$ remains.
5. If a new candidate's $M$ exceeds the lowest-$M$ current resident, evict the resident and admit the candidate.
6. A request is a warm hit when the tenant's true next tool is in the warm pool that timestep.

**Known limitation:** greedy-by-density is not provably optimal for indivisible (0/1) capacity allocation — it can occasionally pick one high-density item that consumes the entire remaining budget over several lower-density items that would jointly fit and sum to more value. This is an accepted limitation for a simulator-scale study; note it alongside "no exact solver" and "single-node capacity" in the paper's limitations section.

## 7. Differentiation

This work studies allocation of a finite shared sandbox budget using tenant-specific probabilistic next-tool forecasts, under cold-start, warm-resource, SLO, and fairness costs. This differs from prior work in three ways:
- Single-agent, single-host prediction/prewarming systems — those optimize one agent's own runtime with no cross-tenant capacity competition.
- Multi-tenant cache-replacement systems — those share containers across tenants using backward-looking access recency/frequency, not forward-looking, forecast-weighted value density.
- Static, interval-based pool-sizing methods — those show that even a perfect predictor fails to meet a target SLO when pool size is recomputed periodically rather than decided per-candidate against a live shared budget; this motivates a per-timestep, per-candidate decision rule instead.

## 8. Open items

- [ ] Confirm $\eta$ (cost-to-memory conversion constant) is consistent with the calibrated cost model in the simulator core.
- [ ] Confirm $R_t$ (prewarm rate limit) doesn't starve burst scenarios in the trace generator.
