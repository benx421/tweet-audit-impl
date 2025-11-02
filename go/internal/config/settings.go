package config

import "os"

// Settings holds configuration for the tweet audit tool.
type Settings struct {
	TweetsArchivePath    string
	ProcessedTweetsPath  string
	ProcessedResultsPath string
	BaseTwitterURL       string
	XUsername            string
	BatchSize            int
}

// New creates a Settings instance with sensible defaults.
func New() Settings {
	username := os.Getenv("X_USERNAME")
	if username == "" {
		username = "Benn_X1"
	}
	return Settings{
		TweetsArchivePath:    "data/tweets/tweets.json",
		ProcessedTweetsPath:  "data/tweets/processed/tweets.csv",
		ProcessedResultsPath: "data/tweets/processed/results.csv",
		BaseTwitterURL:       "https://x.com",
		XUsername:            username,
		BatchSize:            1000,
	}
}

// TweetURL constructs the full URL for a tweet.
func (s *Settings) TweetURL(tweetID string) string {
	return s.BaseTwitterURL + "/" + s.XUsername + "/status/" + tweetID
}
