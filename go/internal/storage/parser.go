package storage

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"

	"github.com/benx421/tweet-audit/impl/go/internal/models"
)

type ParserType string

const (
	ParserTypeJSON ParserType = "json"
	ParserTypeCSV  ParserType = "csv"
)

func (t ParserType) isValid() bool {
	switch t {
	case ParserTypeCSV, ParserTypeJSON:
		return true
	}
	return false
}

func (t ParserType) String() string {
	return string(t)
}

type tweetWrapper struct {
	Tweet models.Tweet `json:"tweet"`
}

// Parser reads and parses Twitter archive files into Tweet objects.
type Parser struct {
	reader     io.ReadCloser
	parserType ParserType
}

func NewParser(path string, parserType ParserType) (*Parser, error) {
	if !parserType.isValid() {
		msg := fmt.Sprintf("invalid parserType: %s", parserType.String())
		return nil, errors.New(msg)
	}

	cleanPath := filepath.Clean(path)
	file, err := os.Open(cleanPath)
	if err != nil {
		return nil, err
	}

	return &Parser{
		parserType: parserType,
		reader:     file,
	}, nil
}

func (p *Parser) Parse() ([]models.Tweet, error) {
	//nolint:errcheck // defer close, error is not actionable
	defer p.close()
	decoder := json.NewDecoder(p.reader)

	var data []tweetWrapper
	err := decoder.Decode(&data)
	if err != nil {
		return nil, err
	}

	tweets := make([]models.Tweet, 0, len(data))
	for _, item := range data {
		tweets = append(tweets, item.Tweet)
	}

	return tweets, nil

}

func (p *Parser) close() error {
	if p.reader != nil {
		return p.reader.Close()
	}
	return nil
}
