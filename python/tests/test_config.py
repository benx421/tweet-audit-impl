import json

from config import _load_config_file, load_settings


def test_should_load_settings_with_env_variables(monkeypatch):
    # Clear cache to allow environment variables to be re-read
    load_settings.cache_clear()

    monkeypatch.setenv("X_USERNAME", "envuser")
    monkeypatch.setenv("GEMINI_API_KEY", "test-key")
    monkeypatch.setenv("GEMINI_MODEL", "gemini-test")
    monkeypatch.setenv("RATE_LIMIT_SECONDS", "2.5")

    settings = load_settings()

    assert settings.x_username == "envuser"
    assert settings.gemini_api_key == "test-key"
    assert settings.gemini_model == "gemini-test"
    assert settings.rate_limit_seconds == 2.5


def test_should_load_settings_with_default_criteria():
    settings = load_settings()

    assert len(settings.criteria.topics_to_exclude) > 0
    assert "Profanity or unprofessional language" in settings.criteria.topics_to_exclude
    assert len(settings.criteria.tone_requirements) > 0


def test_should_load_criteria_from_config_file(tmp_path):
    config_file = tmp_path / "config.json"
    config_data = {
        "criteria": {
            "additional_instructions": "Custom instructions",
            "forbidden_words": ["word1", "word2"],
            "topics_to_exclude": ["Topic1", "Topic2"],
            "tone_requirements": ["Tone1"],
        }
    }
    with open(config_file, "w", encoding="utf-8") as f:
        json.dump(config_data, f)

    criteria = _load_config_file(str(config_file))

    assert criteria is not None
    assert criteria.additional_instructions == "Custom instructions"
    assert criteria.forbidden_words == ["word1", "word2"]
    assert criteria.topics_to_exclude == ["Topic1", "Topic2"]
    assert criteria.tone_requirements == ["Tone1"]


def test_should_return_none_when_config_file_missing():
    criteria = _load_config_file("nonexistent.json")

    assert criteria is None


def test_should_return_none_when_config_file_invalid(tmp_path):
    config_file = tmp_path / "invalid.json"
    config_file.write_text("not valid json{")

    criteria = _load_config_file(str(config_file))

    assert criteria is None


def test_should_handle_missing_criteria_fields_in_config(tmp_path):
    config_file = tmp_path / "partial.json"
    config_data = {"criteria": {"forbidden_words": ["test"]}}
    with open(config_file, "w", encoding="utf-8") as f:
        json.dump(config_data, f)

    criteria = _load_config_file(str(config_file))

    assert criteria is not None
    assert criteria.forbidden_words == ["test"]
    assert criteria.topics_to_exclude == []
    assert criteria.tone_requirements == []
    assert criteria.additional_instructions == ""
