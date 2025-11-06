from dataclasses import dataclass


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
    should_delete: bool = False

    def __repr__(self) -> str:
        return f"AnalysisResult(tweet_url={self.tweet_url!r}, should_delete={self.should_delete})"


@dataclass(frozen=True)
class ExtractionResult:
    success: bool
    count: int = 0
    error_type: str = ""
    error_message: str = ""

    def __repr__(self) -> str:
        if self.success:
            return f"ExtractionResult(success=True, count={self.count})"
        return f"ExtractionResult(success=False, error_type={self.error_type!r})"
