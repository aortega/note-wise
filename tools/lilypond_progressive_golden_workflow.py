#!/usr/bin/env python3
"""Progressive approval workflow for LilyPond visual goldens.

This tool supports a human-in-the-loop loop:
1) run one fixture
2) review candidate image
3) approve (promote) or reject
4) continue with next fixture
"""

from __future__ import annotations

import argparse
import json
import subprocess
import shutil
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from PIL import Image, ImageDraw


REPO_ROOT = Path(__file__).resolve().parents[1]
NOTEWISE_ROOT = REPO_ROOT / "android" / "NoteWise"

META_PATH = NOTEWISE_ROOT / "app" / "src" / "main" / "assets" / "samples" / "lilypond_tests" / "test_metadata.json"
REF_IMAGES_DIR = NOTEWISE_ROOT / "app" / "src" / "main" / "assets" / "samples" / "lilypond_tests" / "images"
GOLDENS_DIR = NOTEWISE_ROOT / "app" / "src" / "test" / "resources" / "visual-goldens" / "lilypond" / "tier1"
VISUAL_REPORT_DIR = NOTEWISE_ROOT / "app" / "build" / "reports" / "visual-tests" / "lilypond" / "tier1"
REVIEW_REPORT_DIR = NOTEWISE_ROOT / "app" / "build" / "reports" / "visual-tests" / "lilypond" / "review"
MANIFEST_PATH = GOLDENS_DIR / "approval_manifest.json"

# Fixture names used by NoteWise tests do not always exactly match metadata titles.
REFERENCE_TITLE_ALIASES = {
    "12aa-Clefs_Pitch_Traditional.xml": "12a-Clefs.xml",
}


@dataclass
class FixtureState:
    fixture_xml: str
    golden_stem: str
    status: str
    reference_image: Optional[str]
    reference_size: Optional[list[int]]
    approved_golden: Optional[str]
    last_candidate: Optional[str]
    note: str
    updated_at: str


# Tier-1 fixtures currently used by LilyPondTier1VisualTest.
FIXTURE_MAP = {
    "01a-Pitches-Pitches.xml": "01a_pitches_pitches",
    "11a-TimeSignatures.xml": "11a_time_signatures",
    "12aa-Clefs_Pitch_Traditional.xml": "12aa_clefs_pitch_traditional",
    "13a-KeySignatures.xml": "13a_key_signatures",
}


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def load_metadata() -> dict[str, dict]:
    entries = json.loads(META_PATH.read_text(encoding="utf-8"))
    by_title: dict[str, dict] = {}
    for entry in entries:
        title = str(entry.get("title", "")).strip()
        xml_filename = str(entry.get("xml_filename", "")).strip()
        image_filename = str(entry.get("image_filename", "")).strip()
        if not title or not xml_filename or not image_filename:
            continue
        by_title[title] = entry
    return by_title


def resolve_reference_entry(metadata: dict[str, dict], fixture_xml: str) -> Optional[dict]:
    # 1) Exact title match first.
    entry = metadata.get(fixture_xml)
    if entry:
        return entry

    # 2) Explicit alias map for known fixture naming drifts.
    alias_title = REFERENCE_TITLE_ALIASES.get(fixture_xml)
    if alias_title:
        entry = metadata.get(alias_title)
        if entry:
            return entry

    # 3) Fallback: same numeric prefix (e.g., "12aa-..." -> "12a-...").
    prefix = fixture_xml.split("-", 1)[0]
    number = "".join(ch for ch in prefix if ch.isdigit())
    if not number:
        return None
    for title, candidate in metadata.items():
        title_prefix = title.split("-", 1)[0]
        title_number = "".join(ch for ch in title_prefix if ch.isdigit())
        if title_number == number:
            return candidate

    return None


def resolve_reference_entry_strict(metadata: dict[str, dict], fixture_xml: str) -> Optional[dict]:
    """Resolve reference entry without fuzzy numeric-prefix fallback.

    Apples-to-apples mode: only exact fixture title or explicit alias is allowed.
    """
    exact = metadata.get(fixture_xml)
    if exact:
        return exact

    alias_title = REFERENCE_TITLE_ALIASES.get(fixture_xml)
    if alias_title:
        return metadata.get(alias_title)

    return None


def detect_reference_size(reference_rel: Optional[str]) -> Optional[list[int]]:
    if not reference_rel:
        return None
    path = REF_IMAGES_DIR / reference_rel
    if not path.exists():
        return None
    with Image.open(path) as im:
        return [im.width, im.height]


def build_default_manifest() -> dict[str, FixtureState]:
    metadata = load_metadata()
    manifest: dict[str, FixtureState] = {}
    for fixture_xml, golden_stem in FIXTURE_MAP.items():
        entry = resolve_reference_entry_strict(metadata, fixture_xml)
        if not entry:
            print(f"Skipping fixture (no exact/alias metadata match): {fixture_xml}")
            continue

        ref_rel = entry.get("image_filename") if entry else None
        ref_size = detect_reference_size(ref_rel)
        if not ref_rel or ref_size is None:
            print(f"Skipping fixture (missing reference image): {fixture_xml} -> {ref_rel}")
            continue

        manifest[fixture_xml] = FixtureState(
            fixture_xml=fixture_xml,
            golden_stem=golden_stem,
            status="pending",
            reference_image=ref_rel,
            reference_size=ref_size,
            approved_golden=None,
            last_candidate=None,
            note="",
            updated_at=now_iso(),
        )
    return manifest


def load_manifest() -> dict[str, FixtureState]:
    if not MANIFEST_PATH.exists():
        return build_default_manifest()

    raw = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    manifest: dict[str, FixtureState] = {}
    for fixture_xml, payload in raw.items():
        manifest[fixture_xml] = FixtureState(**payload)

    # Auto-merge any newly added fixtures and backfill missing metadata fields.
    defaults = build_default_manifest()

    # Drop fixtures no longer eligible (e.g., missing strict reference image mapping).
    stale_fixtures = [fixture_xml for fixture_xml in manifest if fixture_xml not in defaults]
    for fixture_xml in stale_fixtures:
        del manifest[fixture_xml]

    for fixture_xml, state in defaults.items():
        if fixture_xml not in manifest:
            manifest[fixture_xml] = state
            continue

        current = manifest[fixture_xml]
        if not current.reference_image and state.reference_image:
            current.reference_image = state.reference_image
        if not current.reference_size and state.reference_size:
            current.reference_size = state.reference_size
        manifest[fixture_xml] = current

    return manifest


def save_manifest(manifest: dict[str, FixtureState]) -> None:
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    payload = {k: asdict(v) for k, v in manifest.items()}
    MANIFEST_PATH.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def find_latest_candidate(golden_stem: str) -> Optional[Path]:
    if not VISUAL_REPORT_DIR.exists():
        return None
    candidates = sorted(VISUAL_REPORT_DIR.glob(f"{golden_stem}_*.new.png"), key=lambda p: p.stat().st_mtime)
    if not candidates:
        return None
    return candidates[-1]


def find_existing_golden(golden_stem: str) -> Optional[Path]:
    candidates = sorted(GOLDENS_DIR.glob(f"{golden_stem}_*.png"))
    if not candidates:
        return None
    return candidates[-1]


def next_review_state(manifest: dict[str, FixtureState]) -> Optional[FixtureState]:
    ordered = [manifest[k] for k in FIXTURE_MAP.keys() if k in manifest]
    state = next((s for s in ordered if s.status == "pending"), None)
    if state is None:
        state = next((s for s in ordered if s.status == "rejected"), None)
    return state


def build_side_by_side_panel(
    fixture_xml: str,
    reference_path: Path,
    candidate_path: Path,
    out_path: Path,
) -> Path:
    out_path.parent.mkdir(parents=True, exist_ok=True)

    ref = Image.open(reference_path).convert("RGB")
    cand = Image.open(candidate_path).convert("RGB")

    # Keep both images at native resolution and place them on equal-size canvases.
    canvas_w = max(ref.width, cand.width)
    canvas_h = max(ref.height, cand.height)

    ref_canvas = Image.new("RGB", (canvas_w, canvas_h), (255, 255, 255))
    cand_canvas = Image.new("RGB", (canvas_w, canvas_h), (255, 255, 255))
    ref_canvas.paste(ref, (0, 0))
    cand_canvas.paste(cand, (0, 0))

    pad = 20
    header_h = 64
    panel_w = ref_canvas.width + cand_canvas.width + (pad * 3)
    panel_h = canvas_h + header_h + (pad * 2)
    panel = Image.new("RGB", (panel_w, panel_h), (248, 248, 248))

    panel.paste(ref_canvas, (pad, header_h))
    panel.paste(cand_canvas, (pad * 2 + ref_canvas.width, header_h))

    draw = ImageDraw.Draw(panel)
    draw.text((pad, 8), f"Fixture: {fixture_xml}", fill=(20, 20, 20))
    draw.text((pad, 32), f"LilyPond reference ({ref.width}x{ref.height})", fill=(30, 30, 30))
    draw.text(
        (pad * 2 + ref_canvas.width, 32),
        f"NoteWise rendered candidate ({cand.width}x{cand.height})",
        fill=(30, 30, 30),
    )

    left_box = (pad - 1, header_h - 1, pad + ref_canvas.width, header_h + ref_canvas.height)
    right_box = (
        pad * 2 + ref_canvas.width - 1,
        header_h - 1,
        pad * 2 + ref_canvas.width + cand_canvas.width,
        header_h + cand_canvas.height,
    )
    draw.rectangle(left_box, outline=(170, 170, 170), width=1)
    draw.rectangle(right_box, outline=(170, 170, 170), width=1)

    panel.save(out_path)
    return out_path


def open_image(path: Path) -> None:
    try:
        subprocess.run(["open", str(path)], check=False)
    except Exception:
        # Non-fatal: users can still open the file manually.
        pass


def approve_fixture(
    manifest: dict[str, FixtureState],
    fixture_xml: str,
    candidate: Path,
    note: str,
) -> Path:
    state = manifest[fixture_xml]
    candidate = candidate if candidate.is_absolute() else (REPO_ROOT / candidate)

    width_token = candidate.stem.replace(f"{state.golden_stem}_", "").replace(".new", "")
    golden_name = f"{state.golden_stem}_{width_token}.png"
    golden_path = GOLDENS_DIR / golden_name
    golden_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(candidate, golden_path)

    state.status = "approved"
    state.approved_golden = str(golden_path.relative_to(REPO_ROOT))
    state.last_candidate = str(candidate.relative_to(REPO_ROOT))
    state.note = note or state.note
    state.updated_at = now_iso()
    manifest[fixture_xml] = state
    save_manifest(manifest)
    return golden_path


def reject_fixture(manifest: dict[str, FixtureState], fixture_xml: str, note: str) -> Optional[Path]:
    state = manifest[fixture_xml]
    latest_candidate = find_latest_candidate(state.golden_stem)
    if latest_candidate:
        state.last_candidate = str(latest_candidate.relative_to(REPO_ROOT))

    state.status = "rejected"
    state.note = note or state.note
    state.updated_at = now_iso()
    manifest[fixture_xml] = state
    save_manifest(manifest)
    return latest_candidate


def cmd_init(_: argparse.Namespace) -> int:
    manifest = build_default_manifest()
    save_manifest(manifest)
    print(f"Initialized progressive approval manifest: {MANIFEST_PATH}")
    return 0


def cmd_status(_: argparse.Namespace) -> int:
    manifest = load_manifest()
    save_manifest(manifest)

    print(f"Manifest: {MANIFEST_PATH}")
    if not manifest:
        print("No eligible fixtures (all missing strict reference image mapping).")
        return 0

    for fixture_xml in FIXTURE_MAP.keys():
        if fixture_xml not in manifest:
            continue
        state = manifest[fixture_xml]
        print(
            f"- {fixture_xml}: status={state.status}, "
            f"reference_size={state.reference_size}, approved_golden={state.approved_golden}, "
            f"last_candidate={state.last_candidate}"
        )
    return 0


def cmd_next(_: argparse.Namespace) -> int:
    manifest = load_manifest()

    # Pending first; then rejected for retry loops.
    next_state = next_review_state(manifest)

    if next_state is None:
        print("All fixtures are approved.")
        return 0

    latest_candidate = find_latest_candidate(next_state.golden_stem)
    if latest_candidate is not None:
        next_state.last_candidate = str(latest_candidate.relative_to(REPO_ROOT))
        next_state.updated_at = now_iso()
        manifest[next_state.fixture_xml] = next_state
        save_manifest(manifest)

    width_hint = next_state.reference_size[0] if next_state.reference_size else 720
    print(f"Next fixture: {next_state.fixture_xml}")
    print(f"Golden stem: {next_state.golden_stem}")
    print(f"Reference image: {next_state.reference_image}")
    print(f"Reference size: {next_state.reference_size}")
    if latest_candidate:
        print(f"Latest candidate: {latest_candidate.relative_to(REPO_ROOT)}")

    print("Run command:")
    print(
        "cd android/NoteWise && "
        f"LILYPOND_FIXTURES=\"{next_state.fixture_xml}\" "
        "LILYPOND_RELAX_SANITY=\"true\" "
        "LILYPOND_STAFF_SPACING=\"7\" "
        f"LILYPOND_VISUAL_WIDTHS=\"{width_hint}\" "
        "./gradlew :app:testDebugUnitTest --tests \"*LilyPondTier1VisualTest*\""
    )
    print("Then review candidate in app/build/reports/visual-tests/lilypond/tier1 and run approve/reject.")
    return 0


def cmd_approve(args: argparse.Namespace) -> int:
    manifest = load_manifest()
    fixture_xml = args.fixture
    if fixture_xml not in manifest:
        raise SystemExit(f"Fixture not found in manifest: {fixture_xml}")

    state = manifest[fixture_xml]
    candidate = Path(args.candidate) if args.candidate else find_latest_candidate(state.golden_stem)
    if candidate is None:
        raise SystemExit(
            "No candidate found. Pass --candidate explicitly or run fixture to produce *.new.png first."
        )

    candidate = candidate if candidate.is_absolute() else (REPO_ROOT / candidate)
    if not candidate.exists():
        raise SystemExit(f"Candidate does not exist: {candidate}")

    golden_path = approve_fixture(manifest, fixture_xml, candidate, args.note or state.note)

    print(f"Approved fixture: {fixture_xml}")
    print(f"Promoted candidate to golden: {golden_path.relative_to(REPO_ROOT)}")
    return 0


def cmd_reject(args: argparse.Namespace) -> int:
    manifest = load_manifest()
    fixture_xml = args.fixture
    if fixture_xml not in manifest:
        raise SystemExit(f"Fixture not found in manifest: {fixture_xml}")

    state = manifest[fixture_xml]
    reject_fixture(manifest, fixture_xml, args.note or state.note)

    print(f"Rejected fixture: {fixture_xml}")
    if state.note:
        print(f"Note: {state.note}")
    return 0


def cmd_review(args: argparse.Namespace) -> int:
    manifest = load_manifest()
    save_manifest(manifest)

    while True:
        state = next_review_state(manifest)
        if state is None:
            print("All fixtures are approved.")
            return 0

        candidate = find_latest_candidate(state.golden_stem)
        if candidate is None:
            width_hint = state.reference_size[0] if state.reference_size else 720
            print(f"No candidate found for {state.fixture_xml}.")
            print("Run this first:")
            print(
                "cd android/NoteWise && "
                f"LILYPOND_FIXTURES=\"{state.fixture_xml}\" "
                "LILYPOND_RELAX_SANITY=\"true\" "
                "LILYPOND_STAFF_SPACING=\"7\" "
                f"LILYPOND_VISUAL_WIDTHS=\"{width_hint}\" "
                "./gradlew :app:testDebugUnitTest --tests \"*LilyPondTier1VisualTest*\""
            )
            return 1

        if not state.reference_image:
            print(f"Missing reference image metadata for {state.fixture_xml}")
            return 1

        reference_path = REF_IMAGES_DIR / state.reference_image
        if not reference_path.exists():
            print(f"Reference image does not exist: {reference_path}")
            return 1

        panel_path = REVIEW_REPORT_DIR / f"{state.golden_stem}.review.png"
        panel_path = build_side_by_side_panel(
            fixture_xml=state.fixture_xml,
            reference_path=reference_path,
            candidate_path=candidate,
            out_path=panel_path,
        )

        if not args.no_open:
            open_image(panel_path)

        print()
        print(f"Fixture: {state.fixture_xml}")
        print(f"Reference: {reference_path.relative_to(REPO_ROOT)}")
        print(f"Candidate: {candidate.relative_to(REPO_ROOT)}")
        print(f"Review panel: {panel_path.relative_to(REPO_ROOT)}")
        print("Decision: [y] approve, [n] reject, [s] stop")

        decision = input("> ").strip().lower()
        while decision not in {"y", "n", "s"}:
            decision = input("Enter y/n/s: ").strip().lower()

        if decision == "s":
            print("Stopped.")
            return 0

        if decision == "y":
            default_note = state.note or "approved via review"
            note = input(f"Approval note [{default_note}]: ").strip() or default_note
            golden_path = approve_fixture(manifest, state.fixture_xml, candidate, note)
            print(f"Approved {state.fixture_xml} -> {golden_path.relative_to(REPO_ROOT)}")
        else:
            default_note = state.note or "needs fixes"
            note = input(f"Reject note [{default_note}]: ").strip() or default_note
            reject_fixture(manifest, state.fixture_xml, note)
            print(f"Rejected {state.fixture_xml}")

        if args.one:
            return 0

        next_action = input("Next fixture? [Enter=yes, s=stop]: ").strip().lower()
        if next_action == "s":
            print("Stopped.")
            return 0


def cmd_reset(args: argparse.Namespace) -> int:
    manifest = load_manifest()
    fixture_xml = args.fixture
    if fixture_xml not in manifest:
        raise SystemExit(f"Fixture not found in manifest: {fixture_xml}")

    state = manifest[fixture_xml]
    state.status = "pending"
    state.note = args.note or ""
    state.updated_at = now_iso()
    manifest[fixture_xml] = state
    save_manifest(manifest)

    print(f"Reset fixture to pending: {fixture_xml}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Progressive LilyPond golden approval workflow")
    sub = parser.add_subparsers(dest="command", required=True)

    p_init = sub.add_parser("init", help="initialize/overwrite manifest from current fixture map")
    p_init.set_defaults(func=cmd_init)

    p_status = sub.add_parser("status", help="print workflow status")
    p_status.set_defaults(func=cmd_status)

    p_next = sub.add_parser("next", help="show next fixture to run and command hints")
    p_next.set_defaults(func=cmd_next)

    p_approve = sub.add_parser("approve", help="approve fixture and promote candidate image to golden")
    p_approve.add_argument("fixture", help="fixture xml name, e.g. 11a-TimeSignatures.xml")
    p_approve.add_argument("--candidate", help="optional candidate image path; latest *.new.png is used by default")
    p_approve.add_argument("--note", default="", help="optional approval note")
    p_approve.set_defaults(func=cmd_approve)

    p_reject = sub.add_parser("reject", help="reject fixture and store review note")
    p_reject.add_argument("fixture", help="fixture xml name")
    p_reject.add_argument("--note", default="", help="reason for rejection")
    p_reject.set_defaults(func=cmd_reject)

    p_reset = sub.add_parser("reset", help="set fixture status back to pending")
    p_reset.add_argument("fixture", help="fixture xml name")
    p_reset.add_argument("--note", default="", help="optional reset note")
    p_reset.set_defaults(func=cmd_reset)

    p_review = sub.add_parser(
        "review",
        help="interactive side-by-side review: y/n decision persisted, then next or stop",
    )
    p_review.add_argument("--one", action="store_true", help="review only one fixture and exit")
    p_review.add_argument("--no-open", action="store_true", help="do not auto-open panel image")
    p_review.set_defaults(func=cmd_review)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
