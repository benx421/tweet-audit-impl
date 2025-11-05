from pathlib import Path

import pytest

from config import Settings

TESTDATA_DIR = Path(__file__).parent / "testdata"


@pytest.fixture
def testdata_dir():
    return TESTDATA_DIR


@pytest.fixture
def mock_settings():
    return Settings(
        base_twitter_url="https://x.com",
        x_username="testuser",
        gemini_api_key="test-key",
        gemini_model="gemini-test",
    )
