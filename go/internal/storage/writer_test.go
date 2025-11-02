package storage

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/benx421/tweet-audit/impl/go/internal/models"
)

func TestNewCSVWriter(t *testing.T) {
	tests := []struct {
		name    string
		path    string
		wantErr bool
	}{
		{
			name:    "valid path",
			path:    "test.csv",
			wantErr: false,
		},
		{
			name:    "path with subdirectory",
			path:    "subdir/test.csv",
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			fullPath := filepath.Join(tmpDir, tt.path)

			writer, err := NewCSVWriter(WriterOptions{
				Path:         fullPath,
				ShouldAppend: false,
			})

			if tt.wantErr {
				if err == nil {
					t.Error("NewCSVWriter() expected error, got nil")
				}
				return
			}

			if err != nil {
				t.Errorf("NewCSVWriter() unexpected error: %v", err)
				return
			}

			if writer == nil {
				t.Error("NewCSVWriter() returned nil writer")
			}

			dir := filepath.Dir(fullPath)
			if _, err := os.Stat(dir); os.IsNotExist(err) {
				t.Errorf("NewCSVWriter() did not create directory: %s", dir)
			}
		})
	}
}

func TestCSVWriter_WriteTweets(t *testing.T) {
	testTweets := []models.Tweet{
		{ID: "123", Text: "First tweet"},
		{ID: "456", Text: "Second tweet"},
	}

	tests := []struct {
		name         string
		tweets       []models.Tweet
		shouldAppend bool
		wantHeader   bool
		setupFile    bool
	}{
		{
			name:         "write new file",
			tweets:       testTweets,
			shouldAppend: false,
			wantHeader:   true,
			setupFile:    false,
		},
		{
			name:         "append to existing file",
			tweets:       testTweets,
			shouldAppend: true,
			wantHeader:   false,
			setupFile:    true,
		},
		{
			name:         "append to non-existent file writes header",
			tweets:       testTweets,
			shouldAppend: true,
			wantHeader:   true,
			setupFile:    false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			csvPath := filepath.Join(tmpDir, "tweets.csv")

			if tt.setupFile {
				initialWriter, err := NewCSVWriter(WriterOptions{
					Path:         csvPath,
					ShouldAppend: false,
				})
				if err != nil {
					t.Fatalf("Setup failed: %v", err)
				}
				if err := initialWriter.WriteTweets([]models.Tweet{{ID: "000", Text: "Initial"}}); err != nil {
					t.Fatalf("Setup write failed: %v", err)
				}
			}

			writer, err := NewCSVWriter(WriterOptions{
				Path:         csvPath,
				ShouldAppend: tt.shouldAppend,
			})
			if err != nil {
				t.Fatalf("NewCSVWriter() failed: %v", err)
			}

			err = writer.WriteTweets(tt.tweets)
			if err != nil {
				t.Errorf("WriteTweets() unexpected error: %v", err)
				return
			}

			content, err := os.ReadFile(csvPath)
			if err != nil {
				t.Fatalf("Failed to read CSV file: %v", err)
			}

			contentStr := string(content)

			if tt.wantHeader && !strings.Contains(contentStr, "id,text") {
				t.Error("WriteTweets() missing header in new file")
			}

			for _, tweet := range tt.tweets {
				if !strings.Contains(contentStr, tweet.ID) {
					t.Errorf("WriteTweets() missing tweet ID: %s", tweet.ID)
				}
			}

			if tt.setupFile && tt.shouldAppend {
				if !strings.Contains(contentStr, "Initial") {
					t.Error("WriteTweets() append mode overwrote existing content")
				}
				lines := strings.Split(strings.TrimSpace(contentStr), "\n")
				if len(lines) < 3 {
					t.Errorf("WriteTweets() append mode got %d lines, want at least 3", len(lines))
				}
			}
		})
	}
}

func TestCSVWriter_EmptyTweets(t *testing.T) {
	tmpDir := t.TempDir()
	csvPath := filepath.Join(tmpDir, "empty.csv")

	writer, err := NewCSVWriter(WriterOptions{
		Path:         csvPath,
		ShouldAppend: false,
	})
	if err != nil {
		t.Fatalf("NewCSVWriter() failed: %v", err)
	}

	if err = writer.WriteTweets([]models.Tweet{}); err != nil {
		t.Errorf("WriteTweets() with empty slice failed: %v", err)
	}

	content, err := os.ReadFile(csvPath)
	if err != nil {
		t.Fatalf("Failed to read CSV file: %v", err)
	}

	if !strings.Contains(string(content), "id,text") {
		t.Error("WriteTweets() with empty slice should still write header")
	}
}

func TestCSVWriter_WithParsedTweets(t *testing.T) {
	parser, err := NewParser("testdata/tweets.json", ParserTypeJSON)
	if err != nil {
		t.Fatalf("NewParser() failed: %v", err)
	}

	tweets, err := parser.Parse()
	if err != nil {
		t.Fatalf("Parse() failed: %v", err)
	}

	tmpDir := t.TempDir()
	csvPath := filepath.Join(tmpDir, "tweets.csv")

	writer, err := NewCSVWriter(WriterOptions{
		Path:         csvPath,
		ShouldAppend: false,
	})
	if err != nil {
		t.Fatalf("NewCSVWriter() failed: %v", err)
	}

	if err = writer.WriteTweets(tweets); err != nil {
		t.Errorf("WriteTweets() failed: %v", err)
	}

	content, err := os.ReadFile(csvPath)
	if err != nil {
		t.Fatalf("Failed to read CSV file: %v", err)
	}

	contentStr := string(content)

	if !strings.Contains(contentStr, "id,text") {
		t.Error("WriteTweets() missing CSV header")
	}

	expectedIDs := []string{"1234567890123456789", "9876543210987654321", "5555555555555555555", "7777777777777777777"}
	for _, id := range expectedIDs {
		if !strings.Contains(contentStr, id) {
			t.Errorf("WriteTweets() missing tweet ID: %s", id)
		}
	}

	if !strings.Contains(contentStr, "golang error handling") {
		t.Error("WriteTweets() missing tweet text content")
	}
	if !strings.Contains(contentStr, "Docker BuildKit") {
		t.Error("WriteTweets() missing multi-line tweet content")
	}
}
