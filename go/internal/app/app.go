package app

import (
	"context"
	"fmt"
	"log"

	"github.com/benx421/tweet-audit/impl/go/internal/analyzer"
	"github.com/benx421/tweet-audit/impl/go/internal/config"
	"github.com/benx421/tweet-audit/impl/go/internal/models"
	"github.com/benx421/tweet-audit/impl/go/internal/storage"
)

type Application struct {
	gemini     *analyzer.Gemini
	cfg        *config.Settings
	checkpoint *storage.Checkpoint
}

func New() (*Application, error) {
	cfg := config.New()
	gemini, err := analyzer.New(cfg)
	if err != nil {
		return nil, err
	}

	return &Application{
		cfg:        cfg,
		gemini:     gemini,
		checkpoint: storage.NewCheckpoint(cfg.CheckpointPath),
	}, nil
}

func (a *Application) ExtractTweets() error {
	parser, err := storage.NewParser(a.cfg.TweetsArchivePath, storage.ParserTypeJSON)
	if err != nil {
		return fmt.Errorf("error creating parser: %w", err)
	}

	tweets, err := parser.Parse()
	if err != nil {
		return fmt.Errorf("failed to parse tweets from %s (file may not be valid JSON): %w",
			a.cfg.TweetsArchivePath, err)
	}

	writer, err := storage.NewCSVWriter(storage.WriterOptions{
		Path:         a.cfg.TransformedTweetsPath,
		ShouldAppend: false,
	})
	if err != nil {
		return fmt.Errorf("creating CSV writer: %w", err)
	}

	if err := writer.WriteTweets(tweets); err != nil {
		return fmt.Errorf("error writing tweets to CSV: %w", err)
	}

	return nil
}

func (a *Application) AnalyzeTweets() error {
	tweets, err := a.parseTransformedTweets()
	if err != nil {
		return fmt.Errorf("failed to parse transformed tweets: %w", err)
	}

	startIdx, err := a.checkpoint.Load()
	if err != nil {
		return fmt.Errorf("failed to load checkpoint: %w", err)
	}

	if startIdx >= len(tweets) {
		log.Println("All tweets already analyzed")
		return nil
	}

	endIdx := min(startIdx+a.cfg.BatchSize, len(tweets))

	log.Printf("Processing tweets %d to %d (total: %d)", startIdx, endIdx-1, len(tweets))

	writer, err := storage.NewCSVWriter(storage.WriterOptions{
		Path:         a.cfg.ProcessedResultsPath,
		ShouldAppend: true,
	})
	if err != nil {
		return fmt.Errorf("failed to create writer: %w", err)
	}
	defer func() {
		if closeErr := writer.Close(); closeErr != nil {
			log.Printf("Failed to close writer: %v", closeErr)
		}
	}()

	ctx := context.Background()

	for i := startIdx; i < endIdx; i++ {
		result, err := a.gemini.Analyze(ctx, &tweets[i])
		if err != nil {
			return fmt.Errorf("failed to analyze tweet %s: %w", tweets[i].ID, err)
		}

		if !result.ShouldDelete {
			continue
		}

		if err := writer.WriteResult(result); err != nil {
			return fmt.Errorf("failed to write result: %w", err)
		}
	}

	if err := a.checkpoint.Save(endIdx); err != nil {
		return fmt.Errorf("error saving checkpoint: %w", err)
	}

	log.Printf("Batch complete! Processed %d tweets (%d/%d total)",
		endIdx-startIdx, endIdx, len(tweets))

	return nil
}

func (a *Application) parseTransformedTweets() ([]models.Tweet, error) {
	parser, err := storage.NewParser(a.cfg.TransformedTweetsPath, storage.ParserTypeCSV)
	if err != nil {
		return nil, err
	}

	return parser.Parse()
}
