"""Greedy marginal-value cross-tenant scheduler."""

from __future__ import annotations

from typing import Any, Dict, Iterable, List, Tuple

from .base_policy import BasePolicy


class ProposedPolicy(BasePolicy):
    """First-cut cross-tenant warm-pool policy.

    At each timestep, every nonzero predicted (tenant, tool) pair is scored by:
    w_i * P(need) * cold_start_cost_avoided - holding_cost.
    The highest-value candidates are admitted greedily, and if a candidate with a
    higher marginal value arrives, the lowest-value current resident is evicted.
    """

    def __init__(self, capacity_mb: float = 64000.0, cold_start_cost_ms: float = 3000.0):
        self.capacity_mb = capacity_mb
        self.cold_start_cost_ms = cold_start_cost_ms

    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        state.setdefault("warm_pool", {})
        state.setdefault("tenant_state", {}).setdefault(event.tenant_id, {})
        state["tenant_state"][event.tenant_id]["weight_w_i"] = getattr(event, "weight_w_i", 1.0)

    def _candidate_score(self, event: Any, tool_id: str, prob: float, footprint_mb: float) -> float:
        weight = getattr(event, "weight_w_i", 1.0)
        holding_cost = 0.05 * footprint_mb
        return weight * prob * self.cold_start_cost_ms - holding_cost

    def _build_batch_candidates(self, state: Dict[str, Any]) -> List[Dict[str, Any]]:
        candidates: List[Dict[str, Any]] = []
        for event in state.get("timestep_candidates", []):
            for tool_id, prob in getattr(event, "predicted_dist", {}).items():
                if prob <= 0.0:
                    continue
                footprint_mb = max(256.0, float(state.get("tool_memory_mb", {}).get(tool_id, 512.0)))
                candidates.append(
                    {
                        "tenant_id": event.tenant_id,
                        "tool_id": tool_id,
                        "prob": float(prob),
                        "weight_w_i": float(getattr(event, "weight_w_i", 1.0)),
                        "footprint_mb": footprint_mb,
                        "marginal_value": self._candidate_score(event, tool_id, float(prob), footprint_mb),
                    }
                )
        candidates.sort(key=lambda item: item["marginal_value"], reverse=True)
        return candidates

    def _eviction_candidates(self, warm_pool: Dict[Tuple[str, str], Dict[str, Any]]) -> List[Tuple[str, str, Dict[str, Any]]]:
        return sorted(
            warm_pool.items(),
            key=lambda item: item[1].get("marginal_value", float("-inf")),
        )

    def _reconcile_warm_pool(self, state: Dict[str, Any]) -> None:
        warm_pool = state.setdefault("warm_pool", {})
        capacity_mb = float(state.get("capacity_mb", self.capacity_mb))
        used_mb = sum(item["footprint_mb"] for item in warm_pool.values())

        for candidate in self._build_batch_candidates(state):
            key = (candidate["tenant_id"], candidate["tool_id"])
            if key in warm_pool:
                warm_pool[key]["marginal_value"] = candidate["marginal_value"]
                used_mb = sum(item["footprint_mb"] for item in warm_pool.values())
                continue

            footprint_mb = candidate["footprint_mb"]
            if used_mb + footprint_mb <= capacity_mb:
                warm_pool[key] = {
                    "footprint_mb": footprint_mb,
                    "marginal_value": candidate["marginal_value"],
                    "tenant_id": candidate["tenant_id"],
                    "tool_id": candidate["tool_id"],
                }
                used_mb += footprint_mb
                continue

            evictions = self._eviction_candidates(warm_pool)
            if not evictions:
                continue

            evicted_key, _, evicted = evictions[0]
            if candidate["marginal_value"] > evicted["marginal_value"]:
                warm_pool.pop(evicted_key, None)
                warm_pool[key] = {
                    "footprint_mb": footprint_mb,
                    "marginal_value": candidate["marginal_value"],
                    "tenant_id": candidate["tenant_id"],
                    "tool_id": candidate["tool_id"],
                }
                used_mb = sum(item["footprint_mb"] for item in warm_pool.values())

    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        self._reconcile_warm_pool(state)

        predicted_tool = max(getattr(event, "predicted_dist", {}).items(), key=lambda item: item[1])[0]
        warm_pool = state["warm_pool"]
        warm = warm_pool.get((event.tenant_id, predicted_tool)) is not None
        decision = {
            "cold_start": not warm,
            "latency_ms": 25.0 if warm else 1800.0,
            "action": "reuse" if warm else "warm_pool_miss",
            "tool_id": predicted_tool,
            "marginal_value": warm_pool.get((event.tenant_id, predicted_tool), {}).get("marginal_value", 0.0),
        }
        return decision

    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        warm_pool = state.setdefault("warm_pool", {})
        if decision["tool_id"] is not None:
            warm_pool[(event.tenant_id, decision["tool_id"])] = {
                "footprint_mb": max(256.0, float(state.get("tool_memory_mb", {}).get(decision["tool_id"], 512.0))),
                "marginal_value": float(decision.get("marginal_value", 0.0)),
                "tenant_id": event.tenant_id,
                "tool_id": decision["tool_id"],
            }
