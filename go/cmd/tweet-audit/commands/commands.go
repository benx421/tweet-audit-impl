package commands

import (
	"fmt"
	"os"

	"github.com/benx421/tweet-audit/impl/go/internal/app"
)

const (
	extractTweetsCommand = "extract-tweets"
	analyzeTweetsCommand = "analyze-tweets"
)

// Execute runs the tweet-audit CLI application.
// It parses command-line arguments and routes to extract-tweets or analyze-tweets commands.
func Execute() error {
	if len(os.Args) < 2 {
		printUsage()
		return nil
	}

	cmd := os.Args[1]

	if cmd != extractTweetsCommand && cmd != analyzeTweetsCommand {
		return fmt.Errorf("unknown command: %s", cmd)
	}

	application, err := app.New()
	if err != nil {
		fmt.Println()
		return err
	}

	switch cmd {
	case extractTweetsCommand:
		fmt.Println("Extracting tweets from archive...")

		if err := application.ExtractTweets(); err != nil {
			return fmt.Errorf("extraction failed: %w", err)
		}

		fmt.Println("Successfully extracted tweets")
		return nil

	case analyzeTweetsCommand:
		fmt.Println("Analyzing tweets...")

		if err := application.AnalyzeTweets(); err != nil {
			return fmt.Errorf("analysis failed: %w", err)
		}

		fmt.Println("Analysis complete!")
		return nil

	default:
		return fmt.Errorf("unknown command: %s", cmd)
	}
}

func printUsage() {
	fmt.Println("Usage: tweet-audit <command>")
	fmt.Println("\nCommands:")
	fmt.Println("  extract-tweets  Extract tweets from Twitter archive")
	fmt.Println("  analyze-tweets  Analyze tweets using Gemini AI")
}
