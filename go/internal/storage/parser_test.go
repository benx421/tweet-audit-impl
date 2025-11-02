package storage

import (
	"strings"
	"testing"
)

func TestNewParser(t *testing.T) {
	tests := []struct {
		name        string
		path        string
		parserType  ParserType
		errContains string
		wantErr     bool
	}{
		{
			name:       "valid JSON parser",
			path:       "testdata/tweets.json",
			parserType: ParserTypeJSON,
			wantErr:    false,
		},
		{
			name:        "invalid parser type",
			path:        "testdata/tweets.json",
			parserType:  ParserType("invalid"),
			wantErr:     true,
			errContains: "invalid parserType",
		},
		{
			name:        "file does not exist",
			path:        "testdata/nonexistent.json",
			parserType:  ParserTypeJSON,
			wantErr:     true,
			errContains: "no such file",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			parser, err := NewParser(tt.path, tt.parserType)

			if tt.wantErr {
				if err == nil {
					t.Errorf("NewParser() expected error, got nil")
					return
				}
				if tt.errContains != "" && !strings.Contains(err.Error(), tt.errContains) {
					t.Errorf("NewParser() error = %v, want error containing %q", err, tt.errContains)
				}
				return
			}

			if err != nil {
				t.Errorf("NewParser() unexpected error: %v", err)
				return
			}

			if parser == nil {
				t.Error("NewParser() returned nil parser")
			}
		})
	}
}

func TestParser_Parse(t *testing.T) {
	tests := []struct {
		name        string
		path        string
		errContains string
		wantTweets  int
		wantErr     bool
	}{
		{
			name:       "real Twitter archive format",
			path:       "testdata/tweets.json",
			wantTweets: 4,
			wantErr:    false,
		},
		{
			name:       "empty array",
			path:       "testdata/empty.json",
			wantTweets: 0,
			wantErr:    false,
		},
		{
			name:        "invalid JSON",
			path:        "testdata/invalid.json",
			wantErr:     true,
			errContains: "invalid character",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			parser, err := NewParser(tt.path, ParserTypeJSON)
			if err != nil {
				t.Fatalf("NewParser() failed: %v", err)
			}

			tweets, err := parser.Parse()

			if tt.wantErr {
				if err == nil {
					t.Errorf("Parse() expected error, got nil")
					return
				}
				if tt.errContains != "" && !strings.Contains(err.Error(), tt.errContains) {
					t.Errorf("Parse() error = %v, want error containing %q", err, tt.errContains)
				}
				return
			}

			if err != nil {
				t.Errorf("Parse() unexpected error: %v", err)
				return
			}

			if len(tweets) != tt.wantTweets {
				t.Errorf("Parse() got %d tweets, want %d", len(tweets), tt.wantTweets)
			}

			if tt.wantTweets > 0 && (tweets[0].ID == "" || tweets[0].Text == "") {
				t.Error("Parse() tweet missing ID or Text")
			}
		})
	}
}
