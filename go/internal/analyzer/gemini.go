package analyzer

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/benx421/tweet-audit/impl/go/internal/config"
	"github.com/benx421/tweet-audit/impl/go/internal/models"
	"google.golang.org/genai"
)

type geminiClient interface {
	GenerateContent(ctx context.Context, model string, contents []*genai.Content, genConfig *genai.GenerateContentConfig) (*genai.GenerateContentResponse, error)
}

type genaiClientAdapter struct {
	models *genai.Models
}

func (a *genaiClientAdapter) GenerateContent(ctx context.Context, model string, contents []*genai.Content, genConfig *genai.GenerateContentConfig) (*genai.GenerateContentResponse, error) {
	return a.models.GenerateContent(ctx, model, contents, genConfig)
}

// Gemini analyzes tweets against user-defined criteria.
type Gemini struct {
	client geminiClient
	cfg    config.Settings
}

type geminiResponse struct {
	Decision string `json:"decision"`
	Reason   string `json:"reason"`
}

// New creates a Gemini analyzer. Requires GEMINI_API_KEY environment variable.
func New() (*Gemini, error) {
	cfg := config.New()
	apiClient, err := genai.NewClient(context.Background(), &genai.ClientConfig{
		APIKey:  cfg.GeminiAPIKey,
		Backend: genai.BackendGeminiAPI})

	if err != nil {
		return nil, err
	}

	return &Gemini{
		client: &genaiClientAdapter{models: apiClient.Models},
		cfg:    cfg,
	}, nil
}

// Analyze evaluates a tweet against the configured criteria.
func (g *Gemini) Analyze(ctx context.Context, tweet *models.Tweet) (models.AnalysisResult, error) {
	prompt := g.buildPrompt(tweet)

	resp, err := g.client.GenerateContent(ctx, g.cfg.GeminiModel, genai.Text(prompt), nil)
	if err != nil {
		return models.AnalysisResult{}, err
	}

	if len(resp.Candidates) == 0 || len(resp.Candidates[0].Content.Parts) == 0 {
		return models.AnalysisResult{}, fmt.Errorf("no response from Gemini")
	}

	responseText := strings.TrimSpace(resp.Candidates[0].Content.Parts[0].Text)

	var geminiResp geminiResponse
	if err := json.Unmarshal([]byte(responseText), &geminiResp); err != nil {
		return models.AnalysisResult{}, err
	}

	shouldDelete := strings.EqualFold(geminiResp.Decision, "DELETE")

	return models.AnalysisResult{
		TweetID:      tweet.ID,
		TweetURL:     g.cfg.TweetURL(tweet.ID),
		ShouldDelete: shouldDelete,
	}, nil
}

func (g *Gemini) buildPrompt(tweet *models.Tweet) string {
	var criteriaList strings.Builder

	idx := 1
	for _, topic := range g.cfg.Criteria.TopicsToExclude {
		fmt.Fprintf(&criteriaList, "%d. %s\n", idx, topic)
		idx++
	}
	for _, tone := range g.cfg.Criteria.ToneRequirements {
		fmt.Fprintf(&criteriaList, "%d. %s\n", idx, tone)
		idx++
	}
	if len(g.cfg.Criteria.ForbiddenWords) > 0 {
		fmt.Fprintf(&criteriaList, "%d. Contains forbidden words: %v\n", idx, g.cfg.Criteria.ForbiddenWords)
	}

	additionalInstructions := ""
	if g.cfg.Criteria.AdditionalInstructions != "" {
		additionalInstructions = "\n\nAdditional guidance: " + g.cfg.Criteria.AdditionalInstructions
	}

	return fmt.Sprintf(`You are evaluating tweets for a professional's Twitter cleanup.

Tweet ID: %s
Tweet: "%s"

Mark for deletion if it violates any of these criteria:
%s%s

Respond in JSON format:
{
  "decision": "DELETE" or "KEEP",
  "reason": "brief explanation"
}`, tweet.ID, tweet.Text, criteriaList.String(), additionalInstructions)
}
