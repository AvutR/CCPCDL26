"""Proposed greedy marginal-value cross-tenant scheduler."""

from __future__ import annotations

from typing import Any, Dict

from .base_policy import BasePolicy


class ProposedPolicy(BasePolicy):
    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        state.setdefault("warm_pool", {})
        state.setdefault("tenant_state", {}).setdefault(event.tenant_id, {})

    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        predicted_tool = max(event.predicted_dist.items(), key=lambda item: item[1])[0]
        warm_pool = state["warm_pool"]
        warm = warm_pool.get((event.tenant_id, predicted_tool)) is not None
        decision = {
            "cold_start": not warm,
            "latency_ms": 25.0 if warm else 1800.0,
            "action": "reuse" if warm else "warm_pool_miss",
            "tool_id": predicted_tool,
        }
        if not warm:
            decision["marginal_value"] = event.weight_w_i * max(event.predicted_dist.values())
        return decision

    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        state["warm_pool"][(event.tenant_id, decision["tool_id"])] = event.timestep
