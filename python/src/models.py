from dataclasses import dataclass
from enum import Enum


class Decision(str, Enum):
    """Tweet analysis decision."""

    DELETE = "DELETE"
    KEEP = "KEEP"


@dataclass(frozen=True)
class Tweet:
    id: str
    content: str

    def __repr__(self) -> str:
        preview = self.content[:50] + "..." if len(self.content) > 50 else self.content
        return f"Tweet(id={self.id!r}, content={preview!r})"


@dataclass(frozen=True)
class AnalysisResult:
    tweet_url: str
    decision: Decision = Decision.KEEP

    def __repr__(self) -> str:
        return f"AnalysisResult(tweet_url={self.tweet_url!r}, decision={self.decision.value})"


@dataclass(frozen=True)
class Result:
    success: bool
    count: int = 0
    error_type: str = ""
    error_message: str = ""

    def __repr__(self) -> str:
        if self.success:
            return f"Result(success=True, count={self.count})"
        return f"Result(success=False, error_type={self.error_type!r})"
