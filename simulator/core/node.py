"""Shared node capacity abstraction."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict


@dataclass
class SharedNode:
    """Single shared-pool capacity abstraction for a node."""

    capacity_mb: float
    warm_pool: Dict[str, float] = field(default_factory=dict)

    def available_mb(self) -> float:
        return self.capacity_mb - sum(self.warm_pool.values())

    def reserve(self, tenant_id: str, tool_id: str, footprint_mb: float) -> bool:
        if self.available_mb() < footprint_mb:
            return False
        self.warm_pool[f"{tenant_id}:{tool_id}"] = footprint_mb
        return True

    def evict(self, tenant_id: str, tool_id: str) -> None:
        key = f"{tenant_id}:{tool_id}"
        self.warm_pool.pop(key, None)

    def clear(self) -> None:
        self.warm_pool.clear()
