package storage

import (
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type Checkpoint struct {
	path string
}

func NewCheckpoint(path string) *Checkpoint {
	cleanedPath := filepath.Clean(path)
	return &Checkpoint{cleanedPath}
}

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

func (c *Checkpoint) Save(idx int) error {
	return os.WriteFile(c.path, []byte(strconv.Itoa(idx)), privateFileMode)
}
