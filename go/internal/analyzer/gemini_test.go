package analyzer

import (
	"context"
	"errors"
	"os"
	"strings"
	"testing"

	"github.com/benx421/tweet-audit/impl/go/internal/config"
	"github.com/benx421/tweet-audit/impl/go/internal/models"
	"google.golang.org/genai"
)

// mockGeminiClient is a mock implementation of geminiClient for testing.
type mockGeminiClient struct {
	response *genai.GenerateContentResponse
	err      error
}

func (m *mockGeminiClient) GenerateContent(ctx context.Context, model string, contents []*genai.Content, genConfig *genai.GenerateContentConfig) (*genai.GenerateContentResponse, error) {
	if m.err != nil {
		return nil, m.err
	}
	return m.response, nil
}

func TestNew(t *testing.T) {
	originalAPIKey := os.Getenv("GEMINI_API_KEY")
	defer func() {
		if err := os.Setenv("GEMINI_API_KEY", originalAPIKey); err != nil {
			t.Errorf("failed to restore GEMINI_API_KEY: %v", err)
		}
	}()

	if err := os.Setenv("GEMINI_API_KEY", "test-api-key"); err != nil {
		t.Fatalf("failed to set GEMINI_API_KEY: %v", err)
	}

	analyzer, err := New()
	if err != nil {
		t.Fatalf("New() failed: %v", err)
	}

	if analyzer == nil {
		t.Fatal("New() returned nil analyzer")
	}
	if analyzer.client == nil {
		t.Error("New() analyzer has nil client")
	}
}

func TestBuildPrompt(t *testing.T) {
	g := &Gemini{
		cfg: config.Settings{
			Criteria: config.Criteria{
				ForbiddenWords: []string{"badword1", "badword2"},
				TopicsToExclude: []string{
					"Political opinions",
					"Controversial statements",
				},
				ToneRequirements: []string{
					"Professional language",
					"Respectful communication",
				},
				AdditionalInstructions: "Flag harmful content",
			},
			BaseTwitterURL: "https://x.com",
			XUsername:      "testuser",
		},
	}

	tweet := &models.Tweet{
		ID:   "123456789",
		Text: "This is a test tweet",
	}

	prompt := g.buildPrompt(tweet)

	if !strings.Contains(prompt, tweet.ID) {
		t.Error("buildPrompt() missing tweet ID")
	}
	if !strings.Contains(prompt, tweet.Text) {
		t.Error("buildPrompt() missing tweet text")
	}
	if !strings.Contains(prompt, "Political opinions") {
		t.Error("buildPrompt() missing topics to exclude")
	}
	if !strings.Contains(prompt, "Professional language") {
		t.Error("buildPrompt() missing tone requirements")
	}
	if !strings.Contains(prompt, "badword1") {
		t.Error("buildPrompt() missing forbidden words")
	}
	if !strings.Contains(prompt, "Flag harmful content") {
		t.Error("buildPrompt() missing additional instructions")
	}
	if !strings.Contains(prompt, `"decision": "DELETE" or "KEEP"`) {
		t.Error("buildPrompt() missing JSON format instruction")
	}
}

func TestBuildPromptEmptyCriteria(t *testing.T) {
	g := &Gemini{
		cfg: config.Settings{
			Criteria: config.Criteria{
				ForbiddenWords:         []string{},
				TopicsToExclude:        []string{},
				ToneRequirements:       []string{},
				AdditionalInstructions: "",
			},
			BaseTwitterURL: "https://x.com",
			XUsername:      "testuser",
		},
	}

	tweet := &models.Tweet{
		ID:   "123456789",
		Text: "This is a test tweet",
	}

	prompt := g.buildPrompt(tweet)

	if !strings.Contains(prompt, tweet.ID) {
		t.Error("buildPrompt() missing tweet ID")
	}
	if !strings.Contains(prompt, tweet.Text) {
		t.Error("buildPrompt() missing tweet text")
	}
	if !strings.Contains(prompt, "You are evaluating tweets") {
		t.Error("buildPrompt() missing base prompt")
	}
}

func TestAnalyze(t *testing.T) {
	tests := []struct {
		mockErr          error
		mockResponse     *genai.GenerateContentResponse
		tweet            *models.Tweet
		name             string
		wantShouldDelete bool
		wantErr          bool
	}{
		{
			name: "DELETE response",
			mockResponse: &genai.GenerateContentResponse{
				Candidates: []*genai.Candidate{
					{
						Content: &genai.Content{
							Parts: []*genai.Part{
								{Text: `{"decision": "DELETE", "reason": "Contains profanity"}`},
							},
						},
					},
				},
			},
			tweet: &models.Tweet{
				ID:   "123",
				Text: "Test tweet",
			},
			wantShouldDelete: true,
			wantErr:          false,
		},
		{
			name: "KEEP response",
			mockResponse: &genai.GenerateContentResponse{
				Candidates: []*genai.Candidate{
					{
						Content: &genai.Content{
							Parts: []*genai.Part{
								{Text: `{"decision": "KEEP", "reason": "Professional content"}`},
							},
						},
					},
				},
			},
			tweet: &models.Tweet{
				ID:   "456",
				Text: "Professional tweet",
			},
			wantShouldDelete: false,
			wantErr:          false,
		},
		{
			name: "case insensitive decision",
			mockResponse: &genai.GenerateContentResponse{
				Candidates: []*genai.Candidate{
					{
						Content: &genai.Content{
							Parts: []*genai.Part{
								{Text: `{"decision": "delete", "reason": "lowercase"}`},
							},
						},
					},
				},
			},
			tweet: &models.Tweet{
				ID:   "789",
				Text: "Test",
			},
			wantShouldDelete: true,
			wantErr:          false,
		},
		{
			name:    "API error",
			mockErr: errors.New("API connection failed"),
			tweet:   &models.Tweet{ID: "999", Text: "Test"},
			wantErr: true,
		},
		{
			name: "empty response",
			mockResponse: &genai.GenerateContentResponse{
				Candidates: []*genai.Candidate{},
			},
			tweet:   &models.Tweet{ID: "111", Text: "Test"},
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockClient := &mockGeminiClient{
				response: tt.mockResponse,
				err:      tt.mockErr,
			}

			g := &Gemini{
				client: mockClient,
				cfg: config.Settings{
					BaseTwitterURL: "https://x.com",
					XUsername:      "testuser",
					GeminiModel:    "gemini-2.0-flash-exp",
					Criteria:       config.Criteria{},
				},
			}

			result, err := g.Analyze(context.Background(), tt.tweet)

			if tt.wantErr {
				if err == nil {
					t.Error("Analyze() expected error, got nil")
				}
				return
			}

			if err != nil {
				t.Errorf("Analyze() unexpected error: %v", err)
				return
			}

			if result.TweetID != tt.tweet.ID {
				t.Errorf("Analyze() TweetID = %v, want %v", result.TweetID, tt.tweet.ID)
			}
			if result.ShouldDelete != tt.wantShouldDelete {
				t.Errorf("Analyze() ShouldDelete = %v, want %v", result.ShouldDelete, tt.wantShouldDelete)
			}
			if !strings.Contains(result.TweetURL, tt.tweet.ID) {
				t.Errorf("Analyze() TweetURL missing tweet ID")
			}
		})
	}
}
