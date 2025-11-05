package storage

import (
	"encoding/csv"
	"io"
	"os"
	"path/filepath"

	"github.com/benx421/tweet-audit/impl/go/internal/models"
)

// privateFileMode restricts file access to owner only (0o600).
// Used for CSV files containing sensitive tweet data.
const privateFileMode os.FileMode = 0o600

// privateDirMode restricts directory access to owner and group (0o750).
// Used for directories containing sensitive data files.
const privateDirMode os.FileMode = 0o750

type WriterOptions struct {
	Path         string
	ShouldAppend bool
}

// CSVWriter writes CSV files with automatic directory creation and secure permissions.
type CSVWriter struct {
	writer     io.WriteCloser
	formatter  *csv.Writer
	skipHeader bool
}

func NewCSVWriter(options WriterOptions) (*CSVWriter, error) {
	cleanedPath := filepath.Clean(options.Path)
	dir := filepath.Dir(cleanedPath)
	if err := os.MkdirAll(dir, privateDirMode); err != nil {
		return nil, err
	}

	fileExists := false
	if _, err := os.Stat(cleanedPath); err == nil {
		fileExists = true
	}

	var file *os.File
	var err error

	if options.ShouldAppend && fileExists {
		file, err = os.OpenFile(cleanedPath, os.O_APPEND|os.O_WRONLY, privateFileMode)
	} else {
		file, err = os.OpenFile(cleanedPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, privateFileMode)
	}

	if err != nil {
		return nil, err
	}

	return &CSVWriter{
		writer:     file,
		formatter:  csv.NewWriter(file),
		skipHeader: options.ShouldAppend && fileExists,
	}, nil
}

func (w *CSVWriter) WriteTweets(tweets []models.Tweet) (err error) {
	defer func() {
		if closeErr := w.Close(); closeErr != nil && err == nil {
			err = closeErr
		}
	}()

	if !w.skipHeader {
		if err := w.formatter.Write([]string{"id", "text"}); err != nil {
			return err
		}
	}

	for _, tweet := range tweets {
		record := []string{tweet.ID, tweet.Text}
		if err := w.formatter.Write(record); err != nil {
			return err
		}
	}

	return nil
}

// WriteResult writes a single analysis result without closing the file.
// It is used for incremental writes with checkpointing.
func (w *CSVWriter) WriteResult(result models.AnalysisResult) (err error) {
	if !w.skipHeader {
		if err := w.formatter.Write([]string{"tweet_url", "deleted"}); err != nil {
			return err
		}
		w.skipHeader = true
	}

	record := []string{result.TweetURL, "false"}

	if err := w.formatter.Write(record); err != nil {
		return err
	}

	w.formatter.Flush()
	return w.formatter.Error()
}

func (w *CSVWriter) Close() error {
	if w.writer == nil {
		return nil
	}
	w.formatter.Flush()

	if err := w.formatter.Error(); err != nil {
		return err
	}

	if err := w.writer.Close(); err != nil {
		return err
	}

	return nil
}
