# Trace Schema (Locked)

This schema is intentionally fixed after Day 1. All policies and simulator components build against it.

## Record format

```text
tenant_id, timestep, true_next_tool, predicted_dist: {tool: prob, ...}, weight_w_i
tool_id, mem_footprint, image_pull_ms, runtime_init_ms, exec_duration_dist
```

## Fields

- `tenant_id`: identifier for each tenant or logical agent workload
- `timestep`: discrete event time index
- `true_next_tool`: the actual tool requested next by the tenant
- `predicted_dist`: probability distribution over imminent tool choices as forecast by the tenant or upstream predictor
- `weight_w_i`: tenant weight used for fairness and accounting
- `tool_id`: identifier for a tool or environment type
- `mem_footprint`: memory footprint associated with the warm sandbox
- `image_pull_ms`: latency cost of pulling the sandbox image or environment artifact
- `runtime_init_ms`: runtime initialization delay after image availability
- `exec_duration_dist`: distribution for execution runtime (used in the simulator cost model)

## Notes

- Forecast accuracy and burst correlation are configurable parameters for the synthetic generator.
- The schema is intentionally compact so that all baselines and the proposed policy can parse the same event stream without custom adapters.
- The simulator may internally expand or enrich these fields, but it should preserve the canonical schema above as the externally visible contract.
