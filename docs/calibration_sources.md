# Calibration Sources

This document records the external evidence that guides the simulation model and parameterization. Do not invent numeric values without matching the cited source or calibrating to a documented distributional range.

## Literature and runtime evidence

- Sandbox cold-start latency: 2–4s typical, up to ~20s for heavy environments
  - Source: SpecBox (arXiv 2607.23933)
- Warm/hibernated resume: <25ms
  - Source: Blaxel production data
- microVM boot (VM-level only): ~200ms
  - Source: Blaxel
- OS-level work share of total latency: 56–74%
  - Source: AgentCgroup (arXiv 2602.09345)
- Peak-to-average memory burst ratio: up to 15.4×
  - Source: AgentCgroup
- Generic FaaS cold start: 100ms–several seconds
  - Source: AWS Lambda published data

## Modeling guidance

- Use heavy-tailed distributions such as lognormal or skewed Weibull families where the data suggests nontrivial tail behavior.
- Keep per-tool memory and init costs aligned with the trace schema.
- Prefer establishing a distributional range over a single fixed value, especially for cold-start latency and warm resume behavior.

## Notes

The simulator should treat these values as calibration anchors rather than exact ground truth. The project's central claim is relative comparison under a simulated model, not absolute end-to-end deployment performance.
