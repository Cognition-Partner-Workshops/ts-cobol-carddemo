# Assessment data

The Migrations board writes `inventory.json` (one entry per program) and
`waves.json` (dependency edges and the wave plan) here. The assessment page
in `harness/site.py` renders whatever is present, and shows empty sections
until then.

`inventory.json`: `{"programs": [{"name", "type", "group", "jcl": [], "copybooks": [],
"datasets": [], "cics_transactions": [], "external_calls": [], "unreferenced": false, "wave": ""}]}`

`waves.json`: `{"edges": [{"from", "to", "via"}], "waves": [{"name", "members": []}]}`
