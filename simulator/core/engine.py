"""Discrete-event simulator engine skeleton.

This module is intentionally lightweight but structured to support a multi-tenant
shared-pool serverless sandbox scheduler with policy hooks for warm, cold, and
eviction decisions.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional


@dataclass
class Event:
    timestep: int
    tenant_id: str
    tool_id: str
    predicted_dist: Dict[str, float]
    true_next_tool: Optional[str] = None
    weight_w_i: float = 1.0


@dataclass
class SimulationResult:
    events_processed: int = 0
    cold_starts: int = 0
    warm_hits: int = 0
    total_latency_ms: float = 0.0
    metrics: Dict[str, Any] = field(default_factory=dict)


class SimulatorEngine:
    """Core event loop for the serverless sandbox scheduler.

    A minimal implementation is provided so the repo can run end-to-end with a
    placeholder policy before richer logic is added.
    """

    def __init__(self, policy: Any, capacity: float = 64.0):
        self.policy = policy
        self.capacity = capacity
        self.state: Dict[str, Any] = {
            "warm_pool": {},
            "tenant_state": {},
            "time": 0,
        }

    def run(self, events: List[Event]) -> SimulationResult:
        result = SimulationResult()
        for event in events:
            self.state["time"] = event.timestep
            self.policy.observe(event, self.state)
            decision = self.policy.decide(event, self.state)

            result.events_processed += 1
            if decision.get("cold_start", False):
                result.cold_starts += 1
                result.total_latency_ms += float(decision.get("latency_ms", 0.0))
            else:
                result.warm_hits += 1

            self.policy.after_decision(event, decision, self.state)

        result.metrics = {
            "capacity": self.capacity,
            "warm_pool_size": len(self.state["warm_pool"]),
            "time_steps": self.state["time"],
        }
        return result
