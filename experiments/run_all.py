"""Orchestration entry point for benchmarking all policies on multiple seeds."""

from __future__ import annotations

from simulator.core.engine import SimulatorEngine
from simulator.policies.reactive import ReactivePolicy
from simulator.traces.generator import generate_trace


def main() -> None:
    for seed in range(5):
        events = generate_trace(seed=seed)
        engine = SimulatorEngine(policy=ReactivePolicy(), capacity=64.0)
        result = engine.run(events)
        print(f"seed={seed}: cold_starts={result.cold_starts}, warm_hits={result.warm_hits}, latency_ms={result.total_latency_ms}")


if __name__ == "__main__":
    main()
