from pathlib import Path

import pytest

TESTDATA_DIR = Path(__file__).parent / "testdata"


@pytest.fixture
def testdata_dir():
    return TESTDATA_DIR
