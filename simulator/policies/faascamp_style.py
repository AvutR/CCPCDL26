"""FaaSCamp-style shared caching baseline."""

from __future__ import annotations

from typing import Any, Dict

from .base_policy import BasePolicy


class FaaSCampStylePolicy(BasePolicy):
    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        state.setdefault("shared_cache", {})
        state.setdefault("tenant_state", {}).setdefault(event.tenant_id, {})

    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        shared_cache = state["shared_cache"]
        chosen_tool = max(event.predicted_dist.items(), key=lambda item: item[1])[0]
        warm = shared_cache.get(chosen_tool) is not None
        return {
            "cold_start": not warm,
            "latency_ms": 1200.0 if not warm else 25.0,
            "action": "shared_cache_hit" if warm else "shared_cache_miss",
            "tool_id": chosen_tool,
        }

    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        state["shared_cache"][decision["tool_id"]] = event.timestep
