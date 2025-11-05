package storage

import (
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

// Checkpoint manages progress tracking for tweet analysis.
// It stores the index of the next tweet to process.
type Checkpoint struct {
	path string
}

// NewCheckpoint creates a Checkpoint for the given file path.
func NewCheckpoint(path string) *Checkpoint {
	cleanedPath := filepath.Clean(path)
	return &Checkpoint{cleanedPath}
}

// Load reads the checkpoint index from disk.
func (c *Checkpoint) Load() (int, error) {
	data, err := os.ReadFile(c.path)
	if err != nil {
		if os.IsNotExist(err) {
			return 0, nil
		}
		return 0, err
	}

	idx, err := strconv.Atoi(strings.TrimSpace(string(data)))
	if err != nil {
		return 0, err
	}

	return idx, nil
}

// Save writes the checkpoint index to disk atomically.
// The index represents the next tweet to process (not the last processed).
func (c *Checkpoint) Save(idx int) error {
	return os.WriteFile(c.path, []byte(strconv.Itoa(idx)), privateFileMode)
}
