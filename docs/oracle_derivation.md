# Oracle Baseline — Closed-Form Derivation

## 1. Setup

Oracle has perfect knowledge of the tenant's true next tool at every timestep — it reads `true_next_tool` directly from the trace rather than using `predicted_dist`. It is still bound by the same shared-capacity constraint as every other method:

$$
\sum_{i,k} m_k\, x_{i,k,t} \le C \qquad \forall t
$$

Oracle's only advantage is zero prediction error — not unlimited memory, not a relaxed constraint.

## 2. Derivation

Since Oracle always sets $x_{i,\,y^{\text{true}}_{i,t},\,t} = 1$ whenever capacity allows:

$$
P(\text{miss}_{i,t}) = 0 \quad \text{always, by construction}
$$

so the cold-start term in the general objective vanishes entirely. Oracle's expected per-tenant cost reduces to:

$$
L^{\text{oracle}}_{i,t} = c^{\text{warm}} + C_{\text{migrate}}\sum_k \text{evict}_{i,k,t}
$$

This is the ceiling every other method's cold-start rate and total cost is measured against.

## 3. Validation checks

1. **Zero-cold-start check:** Oracle's cold-start rate must be provably 0% whenever $\sum_i m_{y^{\text{true}}_{i,t}} \le C$ at every $t$ — i.e., whenever the true next-tool working set fits in capacity at that timestep.

2. **Capacity-pressure check:** any nonzero cold-start rate for Oracle should appear only when $\sum_i m_{y^{\text{true}}_{i,t}} > C$ (genuine capacity pressure) and should exactly match the count of forced evictions under that pressure.

3. **Ceiling check (cross-method sanity):** in every full run, Oracle's cold-start rate and total cost must be ≤ every other method's (reactive, independent-prewarm, cache-replacement baseline, proposed policy).

## 4. Integration notes

- Oracle reads `true_next_tool` from the trace schema directly; it does not consume `predicted_dist` at all.
- Oracle uses the same calibrated cost model ($c^{\text{warm}}$, $C_{\text{migrate}}$) as every other method — it should not read from a separate/simplified cost path. $P(\text{miss})$ is forced to 0 by Oracle's decision rule, not by a special-cased cost formula.
- Oracle's admission decision should use the same shared policy interface as every other method, differing only in what it computes as the value/priority score.

## 5. Scope

Oracle is a ceiling under the simulated model, not a real-world performance bound — it inherits every simplification disclosed elsewhere (single shared node, no control-plane delay modeling, synthetic calibrated traces). Conclusions drawn from the Oracle gap should be framed as relative comparisons within this simulated setting, not as absolute real-world claims.
