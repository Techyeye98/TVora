"""Every device note under docs/devices/ is linked from a page a reader can reach.

docs/devices/ is where a device report lives until someone has lived with a list long enough to
promote it into examples/. examples/README.md is the index readers come through: each row of its
table links the note that belongs to that list, and the section "Notes for models that have no
tested list yet" links the reports that have no list at all. A note in neither place is only found
by browsing the folder on GitHub, and three of them were: the Sharp 32FI4EA report, the TCL/FFalcon
MT9655 China report (whose example row never got the link its siblings have) and the MiTV-MTEQ0
baymax firmware notes. This walks the markdown once and fails on the notes nothing points at.

What counts as a reference: markdown links and images, reference definitions and `href`/`src` in raw
HTML - the destinations a reader can click, resolved relative to the page carrying them. What does
not: `http(s)`, `mailto:`, `data:`, a bare `#anchor`, and the insides of fenced code blocks, where a
path is an example rather than a link. Anchors are ignored: `note.md#section` still points at the
file, and a heading's slug is GitHub's, not ours.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import os
import re
import tempfile
import unittest
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
NOTES = "docs/devices"

# Not documentation: the checkout's git data, the tools venv CI builds before this job runs
# (site-packages ships its own .md files), Gradle's output and a theme's node_modules.
SKIP = {".git", "venv", "node_modules", "build", ".gradle", "__pycache__"}

MARKDOWN_LINK = re.compile(r"\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
REFERENCE_DEF = re.compile(r"^\[[^\]]+\]:\s*(\S+)", re.M)
HTML_ATTR = re.compile(r"(?:href|src)=\"([^\"]+)\"")
FENCE = re.compile(r"```.*?```", re.S)
EXTERNAL = ("http://", "https://", "mailto:", "data:", "#", "<")


def markdown_files(root):
    """Every .md below root, outside the folders in SKIP, in a stable order."""
    root = Path(root)
    for dirpath, dirnames, filenames in os.walk(root):
        # Prune instead of filtering after the walk: a venv holds thousands of files we never read.
        dirnames[:] = sorted(name for name in dirnames if name not in SKIP)
        for name in sorted(filenames):
            if name.endswith(".md"):
                yield Path(dirpath) / name


def destinations(text):
    """The link destinations in one page: inline links/images, reference definitions, HTML."""
    text = FENCE.sub("", text)
    return MARKDOWN_LINK.findall(text) + REFERENCE_DEF.findall(text) + HTML_ATTR.findall(text)


def referenced(root):
    """Repository-relative paths of every file the markdown links to, as posix strings."""
    # Resolved like the link targets below: on macOS the temporary directory /var/folders/... is a symlink to
    # /private/var/folders/..., and an unresolved root put every target outside it.
    root = Path(root).resolve()
    out = set()
    for page in markdown_files(root):
        for target in destinations(page.read_text(encoding="utf-8", errors="replace")):
            if target.startswith(EXTERNAL):
                continue
            # A link may carry an anchor of its own ("note.md#section"): the file is still the target.
            local = target.split("#")[0]
            if not local:
                continue
            resolved = (page.parent / unquote(local)).resolve()
            out.add(os.path.relpath(resolved, root).replace(os.sep, "/"))
    return out


def unlinked_notes(root):
    """Repository-relative paths of the files under docs/devices/ that no markdown file links to."""
    root = Path(root)
    linked = referenced(root)
    out = []
    for name in sorted(os.listdir(root / NOTES)):
        path = root / NOTES / name
        if not path.is_file():
            continue
        rel = f"{NOTES}/{name}"
        if rel not in linked:
            out.append(rel)
    return out


class DeviceNotesLinkedTest(unittest.TestCase):
    def test_every_device_note_is_linked(self):
        unlinked = unlinked_notes(ROOT)
        message = "\n".join(unlinked)
        self.assertEqual([], unlinked, "notes nothing links to (add them to examples/README.md, "
                                       "either to the row of their list or to the notes index):\n" + message)

    def test_a_note_nothing_links_to_is_reported(self):
        # The checker has to be able to fail, or the test above proves nothing.
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / NOTES).mkdir(parents=True)
            (root / NOTES / "orphan.md").write_text("# Orphan\n", encoding="utf-8")
            (root / "README.md").write_text("[list](examples/other/debloat.txt)\n", encoding="utf-8")
            self.assertEqual([f"{NOTES}/orphan.md"], unlinked_notes(root))

    def test_a_row_that_links_its_note_counts(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / NOTES).mkdir(parents=True)
            (root / NOTES / "baymax.md").write_text("# Notes\n", encoding="utf-8")
            (root / "examples").mkdir()
            (root / "examples" / "README.md").write_text(
                "| `xiaomi-mitv-mteq0/` | Xiaomi | Android 10 | list; "
                "[notes](../docs/devices/baymax.md) |\n", encoding="utf-8")
            self.assertEqual([], unlinked_notes(root))

    def test_a_note_named_outside_a_link_is_still_reported(self):
        # A path shown in a command, a bare backticked path or an external URL is not a way through.
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / NOTES).mkdir(parents=True)
            (root / NOTES / "sharp-32fi4ea.md").write_text("# Sharp\n", encoding="utf-8")
            (root / "README.md").write_text(
                "```\nsee docs/devices/sharp-32fi4ea.md\n```\n"
                "`docs/devices/sharp-32fi4ea.md`\n"
                "[upstream](https://github.com/Gathin23/cleanyourtv/blob/main/docs/devices/sharp-32fi4ea.md)\n",
                encoding="utf-8")
            self.assertEqual([f"{NOTES}/sharp-32fi4ea.md"], unlinked_notes(root))

    def test_an_anchor_or_an_encoded_space_in_the_destination_still_counts(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / NOTES).mkdir(parents=True)
            (root / NOTES / "note one.md").write_text("# Note\n", encoding="utf-8")
            (root / "README.md").write_text("[notes](docs/devices/note%20one.md)\n", encoding="utf-8")
            self.assertEqual([], unlinked_notes(root))

    def test_build_folders_are_skipped(self):
        # site-packages inside the CI venv links to plenty of markdown that is not ours.
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / NOTES).mkdir(parents=True)
            (root / NOTES / "note.md").write_text("# Note\n", encoding="utf-8")
            (root / "venv" / "lib").mkdir(parents=True)
            (root / "venv" / "lib" / "third-party.md").write_text(
                "[note](../../docs/devices/note.md)\n", encoding="utf-8")
            self.assertEqual([f"{NOTES}/note.md"], unlinked_notes(root))


if __name__ == "__main__":
    unittest.main()
