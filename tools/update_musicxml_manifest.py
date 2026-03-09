#!/usr/bin/env python3
"""
Updates android/.../visual-goldens/musicxml-suite/approval_manifest.json
after regenerating golden PNGs from the alphaTab MusicXMLTestSuite.

Called by tools/regenerate_musicxml_goldens.sh.
"""
import argparse
import json
import os
import struct
from datetime import datetime, timezone


def png_dimensions(path: str):
    with open(path, "rb") as f:
        if f.read(8) != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"Not a PNG: {path}")
        f.read(4)
        if f.read(4) != b"IHDR":
            raise ValueError(f"IHDR not found in {path}")
        data = f.read(13)
    return struct.unpack(">I", data[0:4])[0], struct.unpack(">I", data[4:8])[0]


def build_golden_stem(alphatab_stem: str) -> str:
    return alphatab_stem.replace("-", "_").lower()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--golden-dir", required=True)
    parser.add_argument("--visual-dir", required=True)
    parser.add_argument("--manifest", required=True)
    a = parser.parse_args()

    existing: dict = {}
    if os.path.exists(a.manifest):
        with open(a.manifest) as f:
            existing = json.load(f)

    updated = {}
    now_iso = datetime.now(tz=timezone.utc).isoformat(timespec="seconds")

    for fname in sorted(os.listdir(a.visual_dir)):
        if not fname.endswith(".png"):
            continue
        at_stem = fname[:-4]
        golden_stem = build_golden_stem(at_stem)
        xml_file = at_stem + (".mxl" if "Compressed-MusicXML" in at_stem else ".xml")

        # Find matching golden PNG: golden_stem_{width}.png
        matches = [
            f for f in os.listdir(a.golden_dir)
            if f.startswith(golden_stem + "_") and f.endswith(".png")
        ]
        if not matches:
            print(f"  WARN: no golden PNG found for {at_stem}")
            continue

        golden_png = sorted(matches)[0]
        golden_path = os.path.join(a.golden_dir, golden_png)
        try:
            w, h = png_dimensions(golden_path)
        except Exception as e:
            print(f"  WARN: {golden_path}: {e}")
            continue

        repo_path = f"android/NoteWise/app/src/test/resources/visual-goldens/musicxml-suite/{golden_png}"
        prior = existing.get(xml_file, {})
        updated[xml_file] = {
            "fixture_xml": xml_file,
            "golden_stem": golden_stem,
            "status": prior.get("status", "approved"),
            "reference_size": [w, h],
            "approved_golden": repo_path,
            "last_candidate": None,
            "note": f"Phase 1: alphaTab golden ({w}x{h}, padding=[7,0,7,0])",
            "updated_at": now_iso,
        }

    with open(a.manifest, "w") as f:
        json.dump(updated, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Manifest updated: {len(updated)} entries -> {a.manifest}")


if __name__ == "__main__":
    main()
