package config

import (
	"encoding/json"
	"os"
	"path/filepath"
)

// Criteria defines what makes a tweet a candidate for deletion.
type Criteria struct {
	AdditionalInstructions string   `json:"additional_instructions"`
	ForbiddenWords         []string `json:"forbidden_words"`
	TopicsToExclude        []string `json:"topics_to_exclude"`
	ToneRequirements       []string `json:"tone_requirements"`
}

// Settings holds configuration for the tweet audit tool.
type Settings struct {
	TweetsArchivePath     string
	TransformedTweetsPath string
	CheckpointPath        string
	ProcessedResultsPath  string
	BaseTwitterURL        string
	XUsername             string
	GeminiAPIKey          string
	GeminiModel           string
	Criteria              Criteria
	BatchSize             int
}

// New creates a Settings instance.
func New() *Settings {
	settings := Settings{
		TweetsArchivePath:     "data/tweets/tweets.json",
		TransformedTweetsPath: "data/tweets/transformed/tweets.csv",
		CheckpointPath:        "data/checkpoint.txt",
		ProcessedResultsPath:  "data/tweets/processed/results.csv",
		BaseTwitterURL:        "https://x.com",
		XUsername:             loadFromEnv("X_USERNAME", "Benn_X1"),
		GeminiAPIKey:          loadFromEnv("GEMINI_API_KEY", ""),
		GeminiModel:           loadFromEnv("GEMINI_MODEL", "gemini-2.5-flash"),
		BatchSize:             10,
		Criteria:              defaultCriteria(),
	}

	if cfg := loadConfigFile("config.json"); cfg != nil {
		settings.Criteria = Criteria{
			ForbiddenWords:         cfg.Criteria.ForbiddenWords,
			TopicsToExclude:        cfg.Criteria.TopicsToExclude,
			ToneRequirements:       cfg.Criteria.ToneRequirements,
			AdditionalInstructions: cfg.Criteria.AdditionalInstructions,
		}
	}

	return &settings
}

func defaultCriteria() Criteria {
	return Criteria{
		ForbiddenWords: []string{},
		TopicsToExclude: []string{
			"Profanity or unprofessional language",
			"Personal attacks or insults",
			"Outdated political opinions",
		},
		ToneRequirements: []string{
			"Professional language only",
			"Respectful communication",
		},
		AdditionalInstructions: "Flag any content that could harm professional reputation",
	}
}

type configFile struct {
	Criteria struct {
		AdditionalInstructions string   `json:"additional_instructions"`
		ForbiddenWords         []string `json:"forbidden_words"`
		TopicsToExclude        []string `json:"topics_to_exclude"`
		ToneRequirements       []string `json:"tone_requirements"`
	} `json:"criteria"`
}

func loadFromEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func loadConfigFile(path string) *configFile {
	cleanPath := filepath.Clean(path)
	data, err := os.ReadFile(cleanPath)
	if err != nil {
		return nil
	}

	var cfg configFile
	if err := json.Unmarshal(data, &cfg); err != nil {
		return nil
	}

	return &cfg
}

// TweetURL constructs the full URL for a tweet.
func (s *Settings) TweetURL(tweetID string) string {
	return s.BaseTwitterURL + "/" + s.XUsername + "/status/" + tweetID
}
