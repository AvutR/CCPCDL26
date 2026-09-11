# Problem Statement

## Motivation

Large language model agents invoke external tools and environments in bursts. This creates a workload pattern in which several tenants may simultaneously predict or require similar sandbox states, but the platform must provision a single finite warm-sandbox pool across all of them.

The core scheduling problem is therefore not only deciding whether a sandbox should be kept warm, but allocating scarce warm capacity to maximize service quality while respecting fairness and cost constraints across tenants.

## Research question

How should a serverless platform jointly decide provisioning, eviction, and warm-cache arbitration when each tenant issues probabilistic forecasts over near-future tool needs and the platform has only a shared warm-sandbox budget?

## Why existing approaches are insufficient

Past work on single-agent prewarming and tool prediction is valuable but does not capture the cross-tenant coupling present in shared multi-tenant infrastructure. A platform-level scheduler must reason about global contention, correlated bursts, and fairness, not only local per-tenant prediction accuracy.

## Design goals

- Reduce cold-start latency on the interactive path
- Keep total warm memory usage within a shared capacity budget
- Preserve per-tenant fairness under weights and minimum-share constraints
- Minimize cost under a calibrated serverless abstraction
- Reason under uncertain forecasts and bursty arrivals

## Scope of this sprint

This project intentionally models a simplified shared-node environment. The goal is to compare a small set of representative methods under the same trace, objective, and cost model so that relative performance can be evaluated fairly.
