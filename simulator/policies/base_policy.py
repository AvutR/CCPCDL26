"""Shared policy interface for all scheduling baselines and the proposed method."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict, Optional


class BasePolicy(ABC):
    """Base interface that all policies must implement.

    The interface is intentionally frozen after Day 1 to prevent method-specific
    shape mismatches across baselines and the proposed scheduler.
    """

    @abstractmethod
    def observe(self, event: Any, state: Dict[str, Any]) -> None:
        """Process one event before a decision is made."""

    @abstractmethod
    def decide(self, event: Any, state: Dict[str, Any]) -> Dict[str, Any]:
        """Return a decision object describing the scheduling action."""

    @abstractmethod
    def after_decision(self, event: Any, decision: Dict[str, Any], state: Dict[str, Any]) -> None:
        """Persist state after a decision is applied."""
