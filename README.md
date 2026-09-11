# Forecast-Driven Shared Sandbox Scheduling for Multi-Tenant LLM Agents

**CCPCDL 2026 submission — deadline Sept 14 (conference Sept 28–30)**
**Status: 3–4 day sprint, started Sept 8.**

## 1. Problem statement

Modern LLM agents access tools via ephemeral, isolated sandboxes deployed on
serverless-style infrastructure shared across many tenants. Permanently
reserved sandboxes are economically unsustainable at scale; on-demand
instantiation exposes multi-second cold starts on the agent's interactive
critical path. Recent work (PASTE, SPORK, SpecBox) shows that an agent's next tool
call is often predictable and can be used to prewarm sandboxes — but each
does this inside a single agent's own runtime, on a single trusted host.
None address how a shared, multi-tenant, scale-to-zero platform should
combine many tenants' predictions to jointly decide provisioning and
eviction under one finite shared budget.

**Research question:** When several tenants each predict different
near-future tool/environment needs, how should a platform allocate a finite
shared warm-sandbox budget to minimize cold-start latency and cost while
preserving tenant fairness and SLOs?

## 2. Formal model (owner: Adersh)

```text
minimize   Σ_t Σ_i [ C_warm · warm_i,t
                    + C_cold · P(miss_i,t)
                    + C_migrate · evict_i,t
                    + C_SLO · 1(SLO_i violated at t) ]

subject to  Σ_i mem_i,t ≤ Capacity_node        (per node, per t)
            warm_i,t ∈ {0,1}
            fairness_i ≥ w_i · min_share        (per tenant weight)
```

Full derivation in `docs/formal_model.md`.

## 3. Methods under comparison

| Method | Role | Owner |
| --- | --- | --- |
| Reactive (no prediction) | Floor baseline | Shalini |
| Independent per-tenant prewarming (SpecBox-style, simplified) | What we differentiate against | Shalini |
| FaaSCamp-style shared caching | Cross-tenant baseline | Jeryl |
| Oracle (perfect next-tool knowledge) | Ceiling | Jeryl |
| Proposed: greedy marginal-value cross-tenant arbitration | Our contribution | Akhil + Adersh |

Cut for time: MPC baseline, RL baseline, live cluster deployment. Note as
future work, not attempted this cycle.

## 3.1 Locked arbitration policy (concrete implementation)

This project locks the arbitration policy to the following first-cut algorithm:

1. For each timestep, collect all nonzero predicted probabilities for every
   (tenant, tool) pair in the batch.
2. Compute expected marginal value using the objective-derived score:
   `value_{i,k,t} = w_i × P_i,k,t(need) × cold_start_cost_avoided − holding_cost_{i,k}`.
3. Sort all candidates by descending marginal value.
4. Greedily admit candidates into the shared warm pool while capacity remains,
   keeping the highest-value entries resident.
5. If a new candidate exceeds the lowest-value current resident, evict the
   lowest-value resident and admit the higher-value candidate.
6. A request is considered a warm hit when the predicted tool for the tenant is
   in the current warm pool for that timestep.

This is directly implementable, requires no solver, and matches the novelty of
turning many per-tenant forecasts into one shared ranking under capacity.

## 4. Repo structure

```text
docs/          problem statement, formal model, trace schema, calibration sources
simulator/
  core/        discrete-event engine + calibrated cost model     — Tarkesh
  traces/      synthetic multi-tenant trace generator             — Shloka
  policies/    shared interface + all 5 methods above             — see table
experiments/   run orchestration, configs, raw results
analysis/      tables, plots, sensitivity sweep
paper/         CCPCDL writeup, section by section
```

**Frozen after Day 1, no exceptions:** `simulator/policies/base_policy.py`
(the shared policy interface) and `docs/trace_schema.md`. Every baseline and
the proposed policy build against these without waiting on each other.

## 5. Trace schema (locked)

```text
tenant_id, timestep, true_next_tool, predicted_dist: {tool: prob, ...}, weight_w_i
tool_id, mem_footprint, image_pull_ms, runtime_init_ms, exec_duration_dist
```

Forecast accuracy and cross-tenant burst correlation are configurable
knobs, not fixed values — this is what drives the sensitivity sweep later
without new code.

## 6. Calibration sources (do not invent numbers — use these)

| Parameter | Value | Source |
| --- | --- | --- |
| Sandbox cold-start latency | 2–4s typical, up to ~20s for heavy environments | SpecBox (arXiv 2607.23933) |
| Warm/hibernated resume | <25ms | Blaxel production data |
| microVM boot (VM-level only) | ~200ms | Blaxel |
| OS-level work share of total latency | 56–74% | AgentCgroup (arXiv 2602.09345) |
| Peak-to-average memory burst ratio | up to 15.4× | AgentCgroup |
| Generic FaaS cold start | 100ms–several seconds | AWS Lambda published data |

Fit heavy-tailed distributions (e.g. lognormal) to these ranges — do not use
single fixed values.

## 7. Evaluation methodology requirements

- Validate the simulator first: run the reactive baseline alone and check its
cold-start rate/latency roughly matches SpecBox/AgentCgroup numbers for the
same scenario before trusting any other result.
- **≥10–15 independent random seeds** per method; report mean ± 95% CI, never
a single run.
- Metrics: P50/P95/P99 latency, cold-start rate, warm memory-hours, total
cost, SLO violations, fairness.
- State all simplifications explicitly in the paper (single shared node, no
control-plane delay modeling, synthetic calibrated traces) — this is
expected practice, not a weakness, if disclosed.
- Frame all conclusions as relative comparisons under the simulated model,
not absolute real-world performance claims.

## 7.1 Locked metrics table

| Metric | Definition | Unit | Lock status |
| --- | --- | --- | --- |
| P50 latency | median latency for completed tool requests | ms | Locked |
| P95 latency | 95th percentile latency | ms | Locked |
| P99 latency | 99th percentile latency | ms | Locked |
| Cold-start rate | fraction of requests requiring a cold sandbox start | % | Locked |
| Warm memory-hours | sum of warm sandbox memory held over time | GB·h | Locked |
| Total cost | warm cost + cold-start penalty + eviction cost + SLO penalty | normalized cost units | Locked |
| SLO violations | requests whose latency exceeds the configured SLO threshold | count / % | Locked |
| Fairness | per-tenant warm-share and weighted allocation balance | ratio | Locked |

The full metric semantics are recorded in [docs/metrics.md](docs/metrics.md).

## 8. Day-by-day plan

| Day | Focus | Key deliverable |
| --- | --- | --- |
| 1 (today) | Lock schema + interface; simulator/trace skeletons; policy pseudocode; start Intro/Problem Statement writing | Simulator runs end-to-end with a no-op policy |
| 2 | Implement all 5 methods against the simulator; Related Work + Methodology writing | All methods runnable, rough numbers |
| 3 | Full runs (all methods × all seeds); results table; sensitivity sweep if time; Results + Simulation Model sections | Final results + sensitivity plot if time allows |
| 4 | Writing only: Discussion, Limitations, Conclusion, formatting, proofreading, submit with buffer | Submission |

**Protect above all else if time runs short:** the 4-way comparison
(reactive vs. independent-prewarm vs. FaaSCamp-style vs. proposed policy)
with Oracle as the ceiling line. Drop the sensitivity sweep before dropping
any of these four.

## 9. Ownership at a glance

| Person | Owns |
| --- | --- |
| Akhil | Proposed policy, overall coordination, writing lead (Intro, Related Work, Methodology, Results, Discussion) |
| Adersh | Formal objective/constraints, Oracle baseline (math side) |
| Tarkesh | Simulator core + calibrated cost model |
| Shalini | Reactive + independent-prewarming baselines |
| Shloka | Trace generator (burstiness, correlation, accuracy knobs) |
| Jeryl | Oracle + FaaSCamp-style baseline, robustness experiments |

## 10. Key citations to have on hand

SpecBox (arXiv 2607.23933) · PASTE (arXiv, "Act While Thinking") · SPORK ·
Maestro — *"Workload-Aware Cross-Cluster Scheduling for LLM-Based Multi-Agent
Systems"* (arXiv 2606.12950 — cite by ID, not name, per naming collision
with an unrelated 2013 locking paper) · AgentCgroup (arXiv 2602.09345) ·
Agentix / NSDI 2026 (formerly "Autellix," arXiv 2502.13965) · FaaSCamp
(arXiv 2408.00957) · SpecFaaS (HPCA 2023) · MegaFlow (arXiv 2601.07526,
training/eval infra, not a serving platform) · FAME — *"Optimizing FaaS
Platforms for MCP-enabled Agentic Workflows"* (arXiv 2601.14735) · "Taming
Cold Starts" MPC scheduling (arXiv 2508.07640, MASCOTS 2025).

## 11. Repo kickoff checklist

- [ ] Lock schema and policy interface
- [ ] Simulate end-to-end no-op scheduling
- [ ] Implement reactive baseline
- [ ] Implement independent prewarm baseline
- [ ] Implement FaaSCamp-style baseline
- [ ] Implement oracle baseline
- [ ] Implement proposed policy
- [ ] Run multi-seed evaluation
- [ ] Generate tables and plots
- [ ] Write paper sections and final submission

## 12. Status notes

This repository is initialized according to the sprint structure in the project brief. The content above is the canonical project overview and should be used as the primary reference for implementation and paper planning.
