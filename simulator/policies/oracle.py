"""Oracle policy with perfect next-tool knowledge."""

from __future__ import annotations

from typing import Any, Dict

from .base_policy import BasePolicy


class OraclePolicy(BasePolicy):
    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        state.setdefault("tenant_state", {}).setdefault(event.tenant_id, {})

    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        warm = state["tenant_state"].get(event.tenant_id, {}).get("last_tool") == event.true_next_tool
        return {
            "cold_start": not warm,
            "latency_ms": 25.0 if warm else 0.0,
            "action": "reuse" if warm else "prewarm",
            "tool_id": event.true_next_tool,
        }

    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        state["tenant_state"][event.tenant_id]["last_tool"] = event.true_next_tool
