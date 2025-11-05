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

func TestLoadFromEnv(t *testing.T) {
	tests := []struct {
		name         string
		key          string
		envValue     string
		defaultValue string
		want         string
	}{
		{
			name:         "returns env value when set",
			key:          "TEST_KEY",
			envValue:     "env_value",
			defaultValue: "default",
			want:         "env_value",
		},
		{
			name:         "returns default when env not set",
			key:          "TEST_KEY_MISSING",
			envValue:     "",
			defaultValue: "default",
			want:         "default",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.envValue != "" {
				if err := os.Setenv(tt.key, tt.envValue); err != nil {
					t.Fatalf("failed to set env: %v", err)
				}
				defer func() {
					if err := os.Unsetenv(tt.key); err != nil {
						t.Errorf("failed to unset env: %v", err)
					}
				}()
			}

			got := loadFromEnv(tt.key, tt.defaultValue)
			if got != tt.want {
				t.Errorf("loadFromEnv() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestNew(t *testing.T) {
	originalUsername := os.Getenv("X_USERNAME")
	originalAPIKey := os.Getenv("GEMINI_API_KEY")
	originalModel := os.Getenv("GEMINI_MODEL")

	defer func() {
		if err := os.Setenv("X_USERNAME", originalUsername); err != nil {
			t.Errorf("failed to restore X_USERNAME: %v", err)
		}
		if err := os.Setenv("GEMINI_API_KEY", originalAPIKey); err != nil {
			t.Errorf("failed to restore GEMINI_API_KEY: %v", err)
		}
		if err := os.Setenv("GEMINI_MODEL", originalModel); err != nil {
			t.Errorf("failed to restore GEMINI_MODEL: %v", err)
		}
	}()

	tests := []struct {
		name         string
		envUsername  string
		envAPIKey    string
		envModel     string
		wantUsername string
		wantModel    string
	}{
		{
			name:         "uses environment variables",
			envUsername:  "envuser",
			envAPIKey:    "test-api-key",
			envModel:     "gemini-1.5-pro",
			wantUsername: "envuser",
			wantModel:    "gemini-1.5-pro",
		},
		{
			name:         "uses defaults when env not set",
			envUsername:  "",
			envAPIKey:    "",
			envModel:     "",
			wantUsername: "Benn_X1",
			wantModel:    "gemini-2.5-flash",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.envUsername != "" {
				if err := os.Setenv("X_USERNAME", tt.envUsername); err != nil {
					t.Fatalf("failed to set X_USERNAME: %v", err)
				}
			} else {
				if err := os.Unsetenv("X_USERNAME"); err != nil {
					t.Fatalf("failed to unset X_USERNAME: %v", err)
				}
			}

			if tt.envAPIKey != "" {
				if err := os.Setenv("GEMINI_API_KEY", tt.envAPIKey); err != nil {
					t.Fatalf("failed to set GEMINI_API_KEY: %v", err)
				}
			} else {
				if err := os.Unsetenv("GEMINI_API_KEY"); err != nil {
					t.Fatalf("failed to unset GEMINI_API_KEY: %v", err)
				}
			}

			if tt.envModel != "" {
				if err := os.Setenv("GEMINI_MODEL", tt.envModel); err != nil {
					t.Fatalf("failed to set GEMINI_MODEL: %v", err)
				}
			} else {
				if err := os.Unsetenv("GEMINI_MODEL"); err != nil {
					t.Fatalf("failed to unset GEMINI_MODEL: %v", err)
				}
			}

			settings := New()

			if settings.XUsername != tt.wantUsername {
				t.Errorf("New() XUsername = %v, want %v", settings.XUsername, tt.wantUsername)
			}
			if settings.GeminiModel != tt.wantModel {
				t.Errorf("New() GeminiModel = %v, want %v", settings.GeminiModel, tt.wantModel)
			}
			if settings.TweetsArchivePath == "" || settings.BatchSize == 0 {
				t.Error("New() missing default values")
			}
			if len(settings.Criteria.TopicsToExclude) == 0 {
				t.Error("New() missing default criteria")
			}
		})
	}
}
