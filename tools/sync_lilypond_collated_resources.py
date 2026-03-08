#!/usr/bin/env python3
"""Sync LilyPond MusicXML regression resources from collated-files.html.

This script:
- downloads the latest collated-files.html index
- extracts test titles and source XML/MXL links
- derives corresponding PNG reference links
- writes refreshed test_metadata.json
- optionally downloads XML/MXL and PNG resources into local assets
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen

BASE_URL = "https://lilypond.org/doc/v2.25/input/regression/musicxml/"
COLLATED_URL = urljoin(BASE_URL, "collated-files.html")

REPO_ROOT = Path(__file__).resolve().parents[1]
ASSETS_ROOT = REPO_ROOT / "android" / "NoteWise" / "app" / "src" / "main" / "assets" / "samples" / "lilypond_tests"
XML_DIR = ASSETS_ROOT / "xml_files"
IMG_DIR = ASSETS_ROOT / "images"
TEST_METADATA_PATH = ASSETS_ROOT / "test_metadata.json"
COLLATED_SNAPSHOT = ASSETS_ROOT / "collated-files.html"

TITLE_RE = re.compile(r"^\d{2}[a-zA-Z0-9-]*-.*\.(xml|mxl)$")
REQUIRED_METADATA_KEYS = ("title", "xml_filename", "image_filename")


class CollatedParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.current_href: str | None = None
        self._text_buf: list[str] = []
        self.entries: list[dict[str, str]] = []

    def handle_starttag(self, tag: str, attrs) -> None:
        if tag != "a":
            return
        attr_map = dict(attrs)
        self.current_href = attr_map.get("href")
        self._text_buf = []

    def handle_data(self, data: str) -> None:
        if self.current_href is not None:
            self._text_buf.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag != "a" or self.current_href is None:
            return

        text = "".join(self._text_buf).strip()
        href = self.current_href.strip()
        self.current_href = None
        self._text_buf = []

        if not text or not TITLE_RE.match(text):
            return
        if not href.endswith(".xml") and not href.endswith(".mxl"):
            return

        full_xml_url = urljoin(COLLATED_URL, href)
        parsed = urlparse(full_xml_url)
        xml_path = Path(parsed.path)

        xml_hash_name = xml_path.name
        png_hash_name = re.sub(r"\.(xml|mxl)$", ".png", xml_hash_name)
        # Keep same two-char shard directory as upstream.
        shard_dir = xml_path.parent.name
        image_rel = f"{shard_dir}/{png_hash_name}"
        image_url = urljoin(BASE_URL, image_rel)

        self.entries.append(
            {
                "title": text,
                "description": "",
                "xml_url": full_xml_url,
                "xml_filename": xml_hash_name,
                "image_url": image_url,
                "image_filename": image_rel,
            }
        )


def fetch_bytes(url: str, timeout: int = 30) -> bytes:
    req = Request(url, headers={"User-Agent": "NoteWise-LilyPondSync/1.0"})
    with urlopen(req, timeout=timeout) as response:
        return response.read()


def download_if_missing_or_changed(url: str, dst: Path) -> tuple[str, int]:
    try:
        data = fetch_bytes(url)
    except HTTPError as exc:
        return (f"http-error-{exc.code}", 0)
    except URLError:
        return ("url-error", 0)

    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists() and dst.read_bytes() == data:
        return ("unchanged", len(data))

    dst.write_bytes(data)
    return ("updated", len(data))


def sanitize_metadata(entries: list[dict[str, str]]) -> tuple[list[dict[str, str]], list[str]]:
    """Keep only entries that have required non-empty reference keys."""
    cleaned: list[dict[str, str]] = []
    dropped: list[str] = []

    for entry in entries:
        missing = [key for key in REQUIRED_METADATA_KEYS if not str(entry.get(key, "")).strip()]
        if missing:
            title = entry.get("title") or "<untitled>"
            dropped.append(f"{title} (missing: {', '.join(missing)})")
            continue
        cleaned.append(entry)

    return cleaned, dropped


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync LilyPond collated resources")
    parser.add_argument("--download", action="store_true", help="download XML/MXL and PNG assets")
    args = parser.parse_args()

    html = fetch_bytes(COLLATED_URL)
    ASSETS_ROOT.mkdir(parents=True, exist_ok=True)
    COLLATED_SNAPSHOT.write_bytes(html)

    collated = CollatedParser()
    collated.feed(html.decode("utf-8", errors="replace"))

    # Keep first occurrence per title.
    dedup: dict[str, dict[str, str]] = {}
    for entry in collated.entries:
        dedup.setdefault(entry["title"], entry)

    metadata = [dedup[k] for k in sorted(dedup.keys())]
    metadata, dropped_entries = sanitize_metadata(metadata)
    TEST_METADATA_PATH.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")

    print(f"Fetched index: {COLLATED_URL}")
    print(f"Saved snapshot: {COLLATED_SNAPSHOT}")
    print(f"Parsed entries: {len(metadata)}")
    print(f"Updated metadata: {TEST_METADATA_PATH}")
    if dropped_entries:
        print(f"Dropped entries with missing keys: {len(dropped_entries)}")
        for dropped in dropped_entries:
            print(f"  - {dropped}")

    if not args.download:
        return 0

    xml_updated = 0
    xml_total = 0
    img_updated = 0
    img_total = 0
    img_missing = 0

    for entry in metadata:
        title = entry["title"]
        xml_dst = XML_DIR / title
        xml_state, _ = download_if_missing_or_changed(entry["xml_url"], xml_dst)
        xml_total += 1
        if xml_state == "updated":
            xml_updated += 1

        image_rel = entry["image_filename"]
        image_dst = IMG_DIR / image_rel
        image_state, _ = download_if_missing_or_changed(entry["image_url"], image_dst)
        img_total += 1
        if image_state == "updated":
            img_updated += 1
        elif image_state.startswith("http-error-404"):
            img_missing += 1

    print(f"XML files checked: {xml_total}, updated: {xml_updated}")
    print(f"Image files checked: {img_total}, updated: {img_updated}, missing(404): {img_missing}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("Interrupted", file=sys.stderr)
        raise SystemExit(130)
