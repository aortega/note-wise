#!/usr/bin/env python3
"""Generate human-inspectable proof artifacts for NoteWise LilyPond tier-1 goldens."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Optional, Tuple

from PIL import Image, ImageChops, ImageDraw, ImageOps, ImageStat


REPO_ROOT = Path(__file__).resolve().parents[1]
NOTEWISE_ROOT = REPO_ROOT / "android" / "NoteWise"
GOLDENS_DIR = NOTEWISE_ROOT / "app" / "src" / "test" / "resources" / "visual-goldens" / "lilypond" / "tier1"
META_PATH = NOTEWISE_ROOT / "app" / "src" / "main" / "assets" / "samples" / "lilypond_tests" / "test_metadata.json"
META_FALLBACK_PATH = NOTEWISE_ROOT / "app" / "src" / "main" / "assets" / "samples" / "lilypond_tests" / "comprehensive_test_metadata.json"
REFERENCE_IMAGES_DIR = NOTEWISE_ROOT / "app" / "src" / "main" / "assets" / "samples" / "lilypond_tests" / "images"
OUT_DIR = NOTEWISE_ROOT / "app" / "build" / "reports" / "visual-proof" / "lilypond-tier1"


FIXTURES = [
    ("01a-Pitches-Pitches.xml", "01a_pitches_pitches_720.png"),
    ("11a-TimeSignatures.xml", "11a_time_signatures_720.png"),
    ("12aa-Clefs_Pitch_Traditional.xml", "12aa_clefs_pitch_traditional_720.png"),
    ("13a-KeySignatures.xml", "13a_key_signatures_720.png"),
]


# Keep workflow and proof generation aligned: only explicit aliases are allowed.
REFERENCE_TITLE_ALIASES = {
    "12aa-Clefs_Pitch_Traditional.xml": "12a-Clefs.xml",
}


def build_metadata_index(path: Path) -> dict[str, dict]:
    """Load metadata and drop entries missing required filename keys."""
    entries = json.loads(path.read_text(encoding="utf-8"))
    indexed: dict[str, dict] = {}
    for entry in entries:
        title = str(entry.get("title", "")).strip()
        xml_filename = str(entry.get("xml_filename", "")).strip()
        image_filename = str(entry.get("image_filename", "")).strip()
        if not title or not xml_filename or not image_filename:
            continue
        indexed[title] = entry
    return indexed


def resolve_reference_entry_strict(metadata: dict[str, dict], fixture_xml: str) -> Optional[dict]:
    """Resolve reference entry without fuzzy fallback.

    Apples-to-apples mode: exact title match or explicit alias only.
    """
    entry = metadata.get(fixture_xml)
    if entry:
        return entry

    alias_title = REFERENCE_TITLE_ALIASES.get(fixture_xml)
    if alias_title:
        return metadata.get(alias_title)

    return None


def ink_bbox_and_count(image: Image.Image) -> Tuple[Optional[Tuple[int, int, int, int]], int]:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    min_x, min_y = width, height
    max_x, max_y = -1, -1
    count = 0

    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 16 and (r < 245 or g < 245 or b < 245):
                count += 1
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)

    if count == 0:
        return None, 0
    return (min_x, min_y, max_x, max_y), count


def normalized_similarity_proxy(reference: Image.Image, golden: Image.Image) -> Tuple[float, Image.Image]:
    common_width, common_height = 1024, 512

    ref_l = reference.convert("L")
    gold_l = golden.convert("L")

    ref_fit = ImageOps.contain(ref_l, (common_width, common_height), method=Image.Resampling.BICUBIC)
    gold_fit = ImageOps.contain(gold_l, (common_width, common_height), method=Image.Resampling.BICUBIC)

    ref_canvas = Image.new("L", (common_width, common_height), 255)
    gold_canvas = Image.new("L", (common_width, common_height), 255)

    ref_canvas.paste(ref_fit, ((common_width - ref_fit.width) // 2, (common_height - ref_fit.height) // 2))
    gold_canvas.paste(gold_fit, ((common_width - gold_fit.width) // 2, (common_height - gold_fit.height) // 2))

    diff = ImageChops.difference(ref_canvas, gold_canvas)
    mean_diff = ImageStat.Stat(diff).mean[0]
    similarity = 1.0 - (mean_diff / 255.0)
    return similarity, diff.convert("RGB")


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    metadata_by_title = build_metadata_index(META_PATH)
    fallback_by_title = build_metadata_index(META_FALLBACK_PATH)

    summary = []

    for fixture_xml, golden_name in FIXTURES:
        golden_path = GOLDENS_DIR / golden_name

        if not golden_path.exists():
            summary.append(
                {
                    "fixture": fixture_xml,
                    "golden": str(golden_path),
                    "error": "missing golden",
                }
            )
            continue

        golden = Image.open(golden_path).convert("RGBA")
        bbox, ink_pixels = ink_bbox_and_count(golden)
        area = golden.width * golden.height
        ink_percent = (ink_pixels / area * 100.0) if area else 0.0

        overlay = golden.convert("RGB")
        draw = ImageDraw.Draw(overlay)
        if bbox:
            draw.rectangle(bbox, outline=(255, 0, 0), width=3)
        overlay.save(OUT_DIR / f"{golden_path.stem}.overlay.png")

        if bbox:
            margin = 12
            x0 = max(0, bbox[0] - margin)
            y0 = max(0, bbox[1] - margin)
            x1 = min(golden.width - 1, bbox[2] + margin)
            y1 = min(golden.height - 1, bbox[3] + margin)
            crop = golden.crop((x0, y0, x1 + 1, y1 + 1)).convert("RGB")
            crop.save(OUT_DIR / f"{golden_path.stem}.crop.png")

        entry = resolve_reference_entry_strict(metadata_by_title, fixture_xml)
        if entry is None:
            entry = resolve_reference_entry_strict(fallback_by_title, fixture_xml)

        reference_rel = entry.get("image_filename") if entry else None
        reference_path = REFERENCE_IMAGES_DIR / reference_rel if reference_rel else None

        similarity = None
        if not reference_path or not reference_path.exists():
            summary.append(
                {
                    "fixture": fixture_xml,
                    "golden": str(golden_path.relative_to(REPO_ROOT)),
                    "error": "missing strict reference image mapping",
                }
            )
            continue

        if bbox:
            reference = Image.open(reference_path).convert("RGB")
            target_h = max(reference.height, golden.height)
            ref_vis = reference.resize((int(reference.width * target_h / reference.height), target_h))
            gold_vis = golden.convert("RGB").resize((int(golden.width * target_h / golden.height), target_h))

            panel = Image.new("RGB", (ref_vis.width + gold_vis.width + 20, target_h), (250, 250, 250))
            panel.paste(ref_vis, (0, 0))
            panel.paste(gold_vis, (ref_vis.width + 20, 0))
            labels = ImageDraw.Draw(panel)
            labels.text((8, 8), "LilyPond reference", fill=(20, 20, 20))
            labels.text((ref_vis.width + 28, 8), "NoteWise golden", fill=(20, 20, 20))
            panel.save(OUT_DIR / f"{golden_path.stem}.side_by_side.png")

            similarity, diff = normalized_similarity_proxy(reference, golden)
            diff.save(OUT_DIR / f"{golden_path.stem}.diff_norm.png")

        summary.append(
            {
                "fixture": fixture_xml,
                "golden": str(golden_path.relative_to(REPO_ROOT)),
                "size": [golden.width, golden.height],
                "ink_pixels": ink_pixels,
                "ink_percent": round(ink_percent, 4),
                "ink_bbox": bbox,
                "reference_image": str(reference_path.relative_to(REPO_ROOT)) if reference_path and reference_path.exists() else None,
                "normalized_similarity_proxy": round(similarity, 4) if similarity is not None else None,
            }
        )

    summary_path = OUT_DIR / "summary.json"
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    print(f"Wrote proof artifacts to: {OUT_DIR}")
    print(f"Summary: {summary_path}")
    for row in summary:
        print(row)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
