package app

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/benx421/tweet-audit/impl/go/internal/config"
	"github.com/benx421/tweet-audit/impl/go/internal/models"
)

type mockAnalyzer struct {
	analyzeFunc func(ctx context.Context, tweet *models.Tweet) (models.AnalysisResult, error)
}

func (m *mockAnalyzer) Analyze(ctx context.Context, tweet *models.Tweet) (models.AnalysisResult, error) {
	if m.analyzeFunc != nil {
		return m.analyzeFunc(ctx, tweet)
	}
	return models.AnalysisResult{
		TweetID:      tweet.ID,
		TweetURL:     "https://x.com/test/status/" + tweet.ID,
		ShouldDelete: false,
	}, nil
}

func setupTestApp(t *testing.T, cfg *config.Settings, analyzer *mockAnalyzer) *Application {
	t.Helper()

	if cfg == nil {
		cfg = &config.Settings{
			TweetsArchivePath:     "testdata/tweets.json",
			TransformedTweetsPath: filepath.Join(t.TempDir(), "tweets.csv"),
			CheckpointPath:        filepath.Join(t.TempDir(), "checkpoint.txt"),
			ProcessedResultsPath:  filepath.Join(t.TempDir(), "results.csv"),
			BaseTwitterURL:        "https://x.com",
			XUsername:             "testuser",
			BatchSize:             10,
			Criteria:              config.Criteria{},
		}
	}

	return &Application{
		gemini:     nil, // Will use mock
		cfg:        cfg,
		checkpoint: nil, // Will create as needed
	}
}

func TestExtractTweets(t *testing.T) {
	tmpDir := t.TempDir()

	archivePath := filepath.Join(tmpDir, "tweets.json")
	archiveContent := `[
		{"tweet": {"id_str": "123", "full_text": "Test tweet 1"}},
		{"tweet": {"id_str": "456", "full_text": "Test tweet 2"}}
	]`
	if err := os.WriteFile(archivePath, []byte(archiveContent), 0o600); err != nil {
		t.Fatalf("failed to create test archive: %v", err)
	}

	cfg := &config.Settings{
		TweetsArchivePath:     archivePath,
		TransformedTweetsPath: filepath.Join(tmpDir, "tweets.csv"),
		CheckpointPath:        filepath.Join(tmpDir, "checkpoint.txt"),
		ProcessedResultsPath:  filepath.Join(tmpDir, "results.csv"),
		BaseTwitterURL:        "https://x.com",
		XUsername:             "testuser",
		BatchSize:             10,
	}

	app := setupTestApp(t, cfg, nil)
	app.gemini = nil // Not needed for extract

	if err := app.ExtractTweets(); err != nil {
		t.Fatalf("ExtractTweets() failed: %v", err)
	}

	if _, err := os.Stat(cfg.TransformedTweetsPath); os.IsNotExist(err) {
		t.Fatal("ExtractTweets() did not create output CSV")
	}

	content, err := os.ReadFile(cfg.TransformedTweetsPath)
	if err != nil {
		t.Fatalf("failed to read output CSV: %v", err)
	}

	csvStr := string(content)
	if !strings.Contains(csvStr, "id,text") {
		t.Error("ExtractTweets() CSV missing header")
	}
	if !strings.Contains(csvStr, "123") || !strings.Contains(csvStr, "456") {
		t.Error("ExtractTweets() CSV missing tweet IDs")
	}
	if !strings.Contains(csvStr, "Test tweet 1") {
		t.Error("ExtractTweets() CSV missing tweet content")
	}
}

func TestExtractTweets_InvalidJSON(t *testing.T) {
	tmpDir := t.TempDir()
	archivePath := filepath.Join(tmpDir, "tweets.json")

	if err := os.WriteFile(archivePath, []byte("not valid json"), 0o600); err != nil {
		t.Fatalf("failed to create test archive: %v", err)
	}

	cfg := &config.Settings{
		TweetsArchivePath:     archivePath,
		TransformedTweetsPath: filepath.Join(tmpDir, "tweets.csv"),
	}

	app := setupTestApp(t, cfg, nil)

	if err := app.ExtractTweets(); err == nil {
		t.Error("ExtractTweets() with invalid JSON should return error")
	}
}

func TestExtractTweets_MissingFile(t *testing.T) {
	cfg := &config.Settings{
		TweetsArchivePath:     "/nonexistent/tweets.json",
		TransformedTweetsPath: filepath.Join(t.TempDir(), "tweets.csv"),
	}

	app := setupTestApp(t, cfg, nil)

	if err := app.ExtractTweets(); err == nil {
		t.Error("ExtractTweets() with missing file should return error")
	}
}

func TestParseTransformedTweets(t *testing.T) {
	tmpDir := t.TempDir()
	csvPath := filepath.Join(tmpDir, "tweets.csv")

	csvContent := "id,text\n123,Test tweet 1\n456,Test tweet 2\n"
	if err := os.WriteFile(csvPath, []byte(csvContent), 0o600); err != nil {
		t.Fatalf("failed to create test CSV: %v", err)
	}

	cfg := &config.Settings{
		TransformedTweetsPath: csvPath,
	}

	app := setupTestApp(t, cfg, nil)

	tweets, err := app.parseTransformedTweets()
	if err != nil {
		t.Fatalf("parseTransformedTweets() failed: %v", err)
	}

	if len(tweets) != 2 {
		t.Errorf("parseTransformedTweets() returned %d tweets, want 2", len(tweets))
	}

	if tweets[0].ID != "123" || tweets[0].Text != "Test tweet 1" {
		t.Errorf("parseTransformedTweets() tweet[0] = {%s, %s}, want {123, Test tweet 1}",
			tweets[0].ID, tweets[0].Text)
	}
}

func TestParseTransformedTweets_MalformedCSV(t *testing.T) {
	tmpDir := t.TempDir()
	csvPath := filepath.Join(tmpDir, "tweets.csv")

	csvContent := "wrong,header\n123,Test\n"
	if err := os.WriteFile(csvPath, []byte(csvContent), 0o600); err != nil {
		t.Fatalf("failed to create test CSV: %v", err)
	}

	cfg := &config.Settings{
		TransformedTweetsPath: csvPath,
	}

	app := setupTestApp(t, cfg, nil)

	_, err := app.parseTransformedTweets()
	if err == nil {
		t.Error("parseTransformedTweets() with malformed CSV should return error")
	}
}
