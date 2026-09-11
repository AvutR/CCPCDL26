# Problem Formulation

We model the scheduling problem as a shared-node optimization over a finite warm-sandbox budget. Each tenant submits a forecast over near-future tool usage, and the platform chooses whether to keep a sandbox warm or let it transition to a cold start. The objective balances warm resource cost, expected cold-start penalties, eviction overhead, and SLO violation risk. This setting is naturally framed as a constrained resource allocation problem with fairness and capacity constraints.

The full mathematical formulation is documented in the formal model and is used as the conceptual basis for the simulator and the proposed policy.
