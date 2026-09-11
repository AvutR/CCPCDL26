# Introduction

The emergence of tool-using LLM agents introduces a new class of serverless scheduling challenges. Unlike static microservices, agent workloads are bursty, context-sensitive, and frequently dominated by short bursts of tool invocation. These bursts create a tension between cost efficiency and latency: reserving a sandbox for every possible tool call is too expensive, while starting one on demand introduces cold-start delays directly into the user-facing path.

This project studies the shared-resource version of this problem. The platform does not manage a single agent runtime in isolation; it operates a finite warm-sandbox pool across many tenants. Each tenant has a forecast over its next tool or environment needs, and the platform must jointly decide which sandboxes to keep warm, which to evict, and how to preserve fairness under constrained capacity.

The central contribution is a scheduler that uses prediction quality and marginal value across tenants to arbitrate warm capacity, rather than treating each tenant independently. This setting is closer to real multi-tenant serverless infrastructure than single-agent local prewarming and provides a principled way to compare baselines and proposed scheduling strategies under a calibrated simulation model.
