package commands

import (
	"os"
	"testing"
)

func TestExecute_NoArguments(t *testing.T) {
	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"tweet-audit"}

	err := Execute()
	if err != nil {
		t.Errorf("Execute() with no args returned error: %v", err)
	}
}

func TestExecute_UnknownCommand(t *testing.T) {
	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()

	os.Args = []string{"tweet-audit", "unknown-command"}

	err := Execute()
	if err == nil {
		t.Error("Execute() with unknown command should return error")
	}

	if err.Error() != "unknown command: unknown-command" {
		t.Errorf("Execute() error = %v, want 'unknown command: unknown-command'", err)
	}
}

func TestPrintUsage(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			t.Errorf("printUsage() panicked: %v", r)
		}
	}()

	printUsage()
}
