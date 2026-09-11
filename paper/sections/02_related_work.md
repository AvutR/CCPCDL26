# Related Work

Prior work in LLM agent tool execution, FaaS scheduling, and warm caching has explored prediction-driven prewarming, burst management, and latency reduction. However, most existing systems optimize within a single runtime or a single trust boundary. This project focuses on the multi-tenant, shared-pool setting where a platform-level scheduler must arbitrate warm capacity across many independent forecasts.

A growing literature on serverless cold-start mitigation emphasizes predictive prewarming, cache reuse, and execution locality. Our work differs by explicitly modeling the coordination problem across tenants under a finite warm-memory budget and fairness constraints.
