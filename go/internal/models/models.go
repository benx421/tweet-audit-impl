package models

// AnalysisResult represents the evaluation outcome for a single tweet.
type AnalysisResult struct {
	TweetID      string
	TweetURL     string
	ShouldDelete bool // True if tweet should be flagged for deletion
}

// Tweet represents a single tweet from the Twitter archive.
type Tweet struct {
	ID   string `json:"id_str"`
	Text string `json:"full_text"`
}
