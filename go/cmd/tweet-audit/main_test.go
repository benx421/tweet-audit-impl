package main

import (
	"bytes"
	"io"
	"os"
	"strings"
	"testing"
)

func TestRun(t *testing.T) {
	old := os.Stdout
	r, w, err := os.Pipe()
	if err != nil {
		t.Fatal(err)
	}
	os.Stdout = w

	run()

	if err := w.Close(); err != nil {
		t.Fatal(err)
	}
	os.Stdout = old

	var buf bytes.Buffer
	if _, err := io.Copy(&buf, r); err != nil {
		t.Fatal(err)
	}
	output := buf.String()

	if !strings.Contains(output, "Welcome to Tweet Audit") {
		t.Errorf("got %q, want output containing 'Welcome to Tweet Audit'", output)
	}
}
