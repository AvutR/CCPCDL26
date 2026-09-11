"""Calibrated cost-model utilities for sandbox scheduling.

The project uses distributional calibration rather than single-point constants in
order to reflect heavy-tailed cold-start behavior and bursty memory demand.
"""

from __future__ import annotations

import random
from typing import Dict, Optional


def sample_cold_start_ms(base_range: tuple[float, float], heavy_tail: float = 1.5) -> float:
    """Sample a heavy-tailed cold-start latency in milliseconds.

    The calibration guidance suggests ranges such as 2–4s typical and up to ~20s
    for heavy environments, so this helper intentionally returns a skewed value.
    """
    low, high = base_range
    # A lognormal-like stretch is used to mimic a temperature-dependent tail.
    center = (low + high) / 2.0
    spread = max((high - low) * 0.35, 200.0)
    value = random.lognormvariate(mean=(center / 1000.0), sigma=heavy_tail)
    return min(max(value * 1000.0, low), high * 1.5)


def sample_resume_ms() -> float:
    """Return a warm resume value consistent with a fast hibernated resume."""
    return random.uniform(10.0, 25.0)


def estimate_memory_cost(footprint_mb: float, duration_steps: int) -> float:
    """Very lightweight warm-memory accounting helper."""
    return footprint_mb * max(duration_steps, 1) / 1000.0


def collapse_tool_distribution(predicted_dist: Optional[Dict[str, float]]) -> Optional[str]:
    if not predicted_dist:
        return None
    return max(predicted_dist.items(), key=lambda item: item[1])[0]
