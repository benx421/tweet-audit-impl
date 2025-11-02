package app

import (
	"fmt"

	"github.com/benx421/tweet-audit/impl/go/internal/config"
	"github.com/benx421/tweet-audit/impl/go/internal/storage"
)

type Application struct {
	cfg config.Settings
}

func New() *Application {
	return &Application{
		cfg: config.New(),
	}
}

func (a *Application) ExtractTweets() error {
	parser, err := storage.NewParser(a.cfg.TweetsArchivePath, storage.ParserTypeJSON)
	if err != nil {
		return fmt.Errorf("creating parser: %w", err)
	}

	tweets, err := parser.Parse()
	if err != nil {
		return fmt.Errorf("parsing tweets from %s (file may not be valid JSON): %w",
			a.cfg.TweetsArchivePath, err)
	}

	writer, err := storage.NewCSVWriter(storage.WriterOptions{
		Path:         a.cfg.ProcessedTweetsPath,
		ShouldAppend: false,
	})
	if err != nil {
		return fmt.Errorf("creating CSV writer: %w", err)
	}

	if err := writer.WriteTweets(tweets); err != nil {
		return fmt.Errorf("writing tweets to CSV: %w", err)
	}

	return nil
}
