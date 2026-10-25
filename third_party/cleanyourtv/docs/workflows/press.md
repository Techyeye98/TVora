# Workflow: Add a press entry

**When to use this:** an article about this project.

1. Add a row `| Outlet | [Headline](<url>) | Date |` to the right country section of `docs/PRESS.md`, in the
   headline's own language. The link must open and show the article: no syndicated copies, no auto-generated
   listings, nothing that cannot be checked.
2. Update the counts in all four places `tests/test_press_counts.py` checks: the `docs/PRESS.md` intro line,
   its index line, the README card's alt text ("and N more: N articles from N countries") and `MORE` in
   `tools/readme_featured.py`. If you cannot redraw `docs/featured.png` and `docs/card.jpg` with
   `tools/readme_featured.py` and `tools/readme_card.py`, say so in the PR.

Then run the tests: `python3 -m unittest discover -s tests -v`.
