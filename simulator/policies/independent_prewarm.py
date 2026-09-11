"""Independent per-tenant prewarming baseline."""

from __future__ import annotations

from typing import Any, Dict

from .base_policy import BasePolicy


class IndependentPrewarmPolicy(BasePolicy):
    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        state.setdefault("tenant_state", {}).setdefault(event.tenant_id, {})

    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        tenant_state = state["tenant_state"].setdefault(event.tenant_id, {})
        predicted_tool = max(event.predicted_dist.items(), key=lambda item: item[1])[0]
        warm = tenant_state.get("warm_tool") == predicted_tool
        return {
            "cold_start": not warm,
            "latency_ms": 2500.0 if not warm else 25.0,
            "action": "prewarm" if not warm else "reuse",
            "tool_id": predicted_tool,
        }

    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        state["tenant_state"][event.tenant_id]["warm_tool"] = decision["tool_id"]
