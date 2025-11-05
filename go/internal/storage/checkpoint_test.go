package storage

import (
	"os"
	"path/filepath"
	"testing"
)

func TestCheckpoint_LoadAndSave(t *testing.T) {
	tmpDir := t.TempDir()
	checkpointPath := filepath.Join(tmpDir, "checkpoint.txt")

	checkpoint := NewCheckpoint(checkpointPath)

	idx, err := checkpoint.Load()
	if err != nil {
		t.Errorf("Load() on non-existent file returned error: %v", err)
	}
	if idx != 0 {
		t.Errorf("Load() on non-existent file = %d, want 0", idx)
	}

	testIdx := 1234
	if saveErr := checkpoint.Save(testIdx); saveErr != nil {
		t.Fatalf("Save(%d) failed: %v", testIdx, saveErr)
	}

	loadedIdx, err := checkpoint.Load()
	if err != nil {
		t.Errorf("Load() after Save() returned error: %v", err)
	}
	if loadedIdx != testIdx {
		t.Errorf("Load() = %d, want %d", loadedIdx, testIdx)
	}

	newIdx := 5678
	if saveErr := checkpoint.Save(newIdx); saveErr != nil {
		t.Fatalf("Save(%d) failed: %v", newIdx, saveErr)
	}

	loadedIdx, err = checkpoint.Load()
	if err != nil {
		t.Errorf("Load() after second Save() returned error: %v", err)
	}
	if loadedIdx != newIdx {
		t.Errorf("Load() after overwrite = %d, want %d", loadedIdx, newIdx)
	}

	content, err := os.ReadFile(checkpointPath)
	if err != nil {
		t.Fatalf("ReadFile() failed: %v", err)
	}
	expected := "5678"
	if string(content) != expected {
		t.Errorf("File content = %q, want %q", string(content), expected)
	}
}

func TestCheckpoint_LoadInvalidFormat(t *testing.T) {
	tmpDir := t.TempDir()
	checkpointPath := filepath.Join(tmpDir, "checkpoint.txt")

	if err := os.WriteFile(checkpointPath, []byte("not-a-number"), 0o600); err != nil {
		t.Fatalf("WriteFile() failed: %v", err)
	}

	checkpoint := NewCheckpoint(checkpointPath)
	_, err := checkpoint.Load()
	if err == nil {
		t.Error("Load() with invalid format should return error")
	}
}

func TestCheckpoint_LoadWithWhitespace(t *testing.T) {
	tmpDir := t.TempDir()
	checkpointPath := filepath.Join(tmpDir, "checkpoint.txt")

	if err := os.WriteFile(checkpointPath, []byte("  42\n"), 0o600); err != nil {
		t.Fatalf("WriteFile() failed: %v", err)
	}

	checkpoint := NewCheckpoint(checkpointPath)
	idx, err := checkpoint.Load()
	if err != nil {
		t.Errorf("Load() with whitespace returned error: %v", err)
	}
	if idx != 42 {
		t.Errorf("Load() = %d, want 42", idx)
	}
}

func TestCheckpoint_SaveZero(t *testing.T) {
	tmpDir := t.TempDir()
	checkpointPath := filepath.Join(tmpDir, "checkpoint.txt")

	checkpoint := NewCheckpoint(checkpointPath)

	if err := checkpoint.Save(0); err != nil {
		t.Fatalf("Save(0) failed: %v", err)
	}

	idx, err := checkpoint.Load()
	if err != nil {
		t.Errorf("Load() after Save(0) returned error: %v", err)
	}
	if idx != 0 {
		t.Errorf("Load() = %d, want 0", idx)
	}
}

func TestNewCheckpoint_CleansPath(t *testing.T) {
	dirtyPath := "/tmp/../tmp/./checkpoint.txt"
	checkpoint := NewCheckpoint(dirtyPath)

	expectedPath := filepath.Clean(dirtyPath)
	if checkpoint.path != expectedPath {
		t.Errorf("NewCheckpoint() path = %q, want %q", checkpoint.path, expectedPath)
	}
}
