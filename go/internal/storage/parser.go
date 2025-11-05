package storage

import (
	"encoding/csv"
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

	switch p.parserType {
	case ParserTypeJSON:
		return p.parseJSON()
	case ParserTypeCSV:
		return p.parseCSV()
	default:
		return nil, fmt.Errorf("invalid parser type: %s", p.parserType)
	}
}

func (p *Parser) close() error {
	if p.reader != nil {
		return p.reader.Close()
	}
	return nil
}

func (p *Parser) parseJSON() ([]models.Tweet, error) {
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

func (p *Parser) parseCSV() ([]models.Tweet, error) {
	reader := csv.NewReader(p.reader)
	headers, err := reader.Read()
	if err != nil {
		return nil, err
	}
	if len(headers) < 2 || headers[0] != "id" || headers[1] != "text" {
		return nil, fmt.Errorf("invalid CSV format: expected 'id,text' headers")
	}

	tweets := make([]models.Tweet, 0, 1024)
	lineNum := 2
	for {
		record, err := reader.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			return nil, err
		}

		if len(record) != 2 {
			return nil, fmt.Errorf("invalid CSV record at line %d: expected 2 fields, got %d", lineNum, len(record))
		}
		tweet := models.Tweet{
			ID:   record[0],
			Text: record[1],
		}
		tweets = append(tweets, tweet)
		lineNum++
	}

	return tweets, nil
}
