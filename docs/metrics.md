# Metrics Table (Locked)

This file defines the evaluation metrics for the simulator and paper. The table is intentionally fixed after the 30-minute sync and should not be changed without explicit reapproval.

| Metric | Definition | Unit | Notes |
| --- | --- | --- | --- |
| P50 latency | Median latency across all requests | ms | Measures central tendency |
| P95 latency | 95th percentile latency | ms | Tail-sensitive user experience |
| P99 latency | 99th percentile latency | ms | Worst-case tail behavior |
| Cold-start rate | Fraction of requests that trigger cold-start provisioning | % | Primary scheduling-quality metric |
| Warm memory-hours | Warm sandbox memory held over time | GB·h | Cost of keeping resources prewarmed |
| Total cost | Warm cost + cold-start penalty + eviction cost + SLO penalty | normalized units | Objective proxy |
| SLO violations | Requests exceeding latency SLO threshold | count or % | Platform-level service guarantee |
| Fairness | Tenant-weighted warm-share balance and allocation equity | ratio | Prevents starvation |

## Implementation notes

- Metrics should be computed over all random seeds and reported as mean ± 95% CI.
- All baselines and the proposed policy must output the same metric schema to preserve comparability.
- Simplifications must be disclosed in the paper, especially if results are relative-under-model only.
