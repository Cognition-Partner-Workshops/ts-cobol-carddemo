"""Shape checks that hold for any run of the batch path, before any rule is written."""


def test_every_daily_record_is_posted_or_rejected(outputs, run_inputs):
    assert len(outputs["posted"]) + len(outputs["rejects"]) == len(run_inputs["dailytran"])


def test_accounts_are_neither_added_nor_dropped(outputs, run_inputs):
    assert set(outputs["accounts"]) == set(run_inputs["accounts"])
