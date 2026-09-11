"""Reactive baseline with no explicit prediction logic."""

from __future__ import annotations

from typing import Any, Dict

from .base_policy import BasePolicy


class ReactivePolicy(BasePolicy):
    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        state.setdefault("tenant_state", {}).setdefault(event.tenant_id, {})

    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        tenant_state = state["tenant_state"].setdefault(event.tenant_id, {})
        tenant_state.setdefault("last_tool", None)
        warm = tenant_state.get("last_tool") == event.tool_id
        return {
            "cold_start": not warm,
            "latency_ms": 3000.0 if not warm else 25.0,
            "action": "provision" if not warm else "reuse",
            "tool_id": event.tool_id,
        }

    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        state["tenant_state"][event.tenant_id]["last_tool"] = event.tool_id
