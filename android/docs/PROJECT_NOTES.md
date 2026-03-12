# Project Notes

## Bravura Metadata File
The file `bravura/redist/bravura_metadata.json` is too large to read directly.
To inspect its contents, use one of the following Python scripts:
- `analyze_bravura_metadata.py`
- `check_engraving_defaults.py`

These scripts are designed to parse and process the large metadata file efficiently.

## 2026-03-11 Follow-Up Notes
- Dotted rest/note augmentation-dot vertical placement may need another calibration pass after broader fixture review; keep this as a potential revisit item.
- Reuse the duration-weighted, bar-constrained spacing compression approach (currently applied for dense rests) for rhythm note durations as well, spanning long/breve/whole through 1024th values.
