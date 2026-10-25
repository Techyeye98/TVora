"""tools/theme.sh finds the theme folder however it is given, without connecting to a TV.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class ThemePathsTest(unittest.TestCase):
    def test_theme_paths(self):
        for path_kind in ("caller-relative", "absolute"):
            with self.subTest(path_kind=path_kind):
                self.check_theme_path(path_kind)

    def test_missing_folder_names_the_path_given(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            tools = self.prepare(base / "repo")
            missing = self.bash_path(base) + "/no-such-theme"
            result, _ = self.run_theme(tools, missing, cwd=base)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn(f"no config.json in {missing}", result.stderr)
            self.assertNotIn("not valid JSON", result.stderr)

    def prepare(self, repo):
        """Copy theme.sh and stand in for tools/venv/bin/python.

        The JSON check and the revision marker are plain `-c` snippets, so they go to the real Python
        and the check is the one theme.sh really runs. tv.py and b64push.py would need a TV: the fake
        records their arguments instead.
        """
        tools = repo / "tools"
        tools.mkdir(parents=True)
        shutil.copy2(ROOT / "tools" / "theme.sh", tools / "theme.sh")
        client = tools / "venv" / "bin" / "python"
        client.parent.mkdir(parents=True)
        client.write_text(
            '#!/bin/sh\n'
            'if [ "$1" = "-I" ] && [ "$2" = "-c" ]; then\n'
            f'  exec "{Path(sys.executable).as_posix()}" "$@"\n'
            'fi\n'
            'shift\n'
            'printf "%s\\n" "$*" >> "$CAPTURE"\n'
            'echo applied\n'
        )
        client.chmod(0o755)
        return tools

    def bash_path(self, path):
        """The path as bash spells it: /tmp/... on Linux and macOS, /c/Users/... under Git Bash."""
        out = subprocess.run(["bash", "-c", 'cd "$1" && pwd', "_", str(path)],
                             capture_output=True, text=True, check=True)
        return out.stdout.strip()

    def run_theme(self, tools, argument, cwd):
        capture = Path(cwd) / "calls.txt"
        env = dict(os.environ, TV_HOST="192.0.2.1", CAPTURE=str(capture))
        result = subprocess.run(["bash", str(tools / "theme.sh"), argument], cwd=cwd, env=env,
                                capture_output=True, text=True, stdin=subprocess.DEVNULL)
        return result, capture

    def check_theme_path(self, path_kind):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            tools = self.prepare(base / "repo")
            theme = base / "caller" / "mytheme"
            theme.mkdir(parents=True)
            (theme / "config.json").write_text(json.dumps({"columns": 5}))
            if path_kind == "absolute":
                argument = self.bash_path(theme)
            else:
                argument = "mytheme"
            result, capture = self.run_theme(tools, argument, cwd=base / "caller")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotIn("not valid JSON", result.stderr)
            calls = capture.read_text()
            push = next(line for line in calls.splitlines() if line.startswith("tv.py --push "))
            pushed = push.split()[2]
            self.assertTrue(pushed.endswith("mytheme/config.json"), push)
            # The path handed to Python must be one Python can open (C:/... rather than /c/... on Windows).
            self.assertTrue(Path(pushed).is_file(), f"Python cannot open {pushed}")


if __name__ == "__main__":
    unittest.main()
