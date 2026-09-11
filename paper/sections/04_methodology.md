# Methodology

The project evaluates a set of representative policies under a common trace schema and shared-node simulator. These methods include reactive scheduling, independent per-tenant prewarming, FaaSCamp-style cross-tenant caching, an oracle ceiling, and the proposed greedy arbitration policy.

Each policy is evaluated on multiple random seeds using the same synthetic but calibrated trace generator. Performance is reported using median and tail latency, cold-start rate, memory usage, SLO violations, and fairness, with confidence intervals over repeated runs.
