package config

import (
	"os"
	"testing"
)

func TestSettings_TweetURL(t *testing.T) {
	settings := Settings{
		BaseTwitterURL: "https://x.com",
		XUsername:      "benx421",
	}

	got := settings.TweetURL("1234567890")
	want := "https://x.com/benx421/status/1234567890"

	if got != want {
		t.Errorf("TweetURL() = %v, want %v", got, want)
	}
}

func TestNew(t *testing.T) {
	tests := []struct {
		name         string
		envUsername  string
		wantUsername string
	}{
		{
			name:         "uses environment variable",
			envUsername:  "envuser",
			wantUsername: "envuser",
		},
		{
			name:         "uses default when env not set",
			envUsername:  "",
			wantUsername: "Benn_X1",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			originalEnv := os.Getenv("X_USERNAME")
			defer func() {
				if err := os.Setenv("X_USERNAME", originalEnv); err != nil {
					t.Errorf("failed to restore env: %v", err)
				}
			}()

			if tt.envUsername != "" {
				if err := os.Setenv("X_USERNAME", tt.envUsername); err != nil {
					t.Fatalf("failed to set env: %v", err)
				}
			} else {
				if err := os.Unsetenv("X_USERNAME"); err != nil {
					t.Fatalf("failed to unset env: %v", err)
				}
			}

			settings := New()

			if settings.XUsername != tt.wantUsername {
				t.Errorf("New() XUsername = %v, want %v", settings.XUsername, tt.wantUsername)
			}
			if settings.TweetsArchivePath == "" || settings.BatchSize == 0 {
				t.Error("New() missing default values")
			}
		})
	}
}
