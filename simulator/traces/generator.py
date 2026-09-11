"""Synthetic trace generator skeleton for multi-tenant, forecasted tool workloads."""

from __future__ import annotations

import random
from typing import Dict, List

from simulator.core.engine import Event


def generate_trace(
    num_tenants: int = 8,
    timesteps: int = 200,
    seed: int = 0,
    forecast_accuracy: float = 0.7,
    burst_correlation: float = 0.2,
) -> List[Event]:
    """Generate a simple synthetic workload with tenant-level bursting.

    The generator is intentionally minimal but exposes the main controllable knobs
    required for later sensitivity analysis.
    """
    random.seed(seed)
    events: List[Event] = []
    tool_pool = ["search", "read", "write", "execute", "summarize", "browser"]

    for t in range(timesteps):
        for tenant_idx in range(num_tenants):
            tool_id = random.choice(tool_pool)
            if random.random() < burst_correlation:
                tool_id = random.choice(tool_pool)

            predicted_dist = {tool: 0.0 for tool in tool_pool}
            predicted_dist[tool_id] = forecast_accuracy
            for other_tool in tool_pool:
                if other_tool != tool_id:
                    predicted_dist[other_tool] = (1.0 - forecast_accuracy) / (len(tool_pool) - 1)

            events.append(
                Event(
                    timestep=t,
                    tenant_id=f"tenant_{tenant_idx}",
                    tool_id=tool_id,
                    true_next_tool=tool_id,
                    predicted_dist=predicted_dist,
                    weight_w_i=1.0 + (tenant_idx % 3) * 0.2,
                )
            )

    return events
