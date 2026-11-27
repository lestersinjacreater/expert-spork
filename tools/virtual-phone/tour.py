#!/usr/bin/env python3
"""Virtual-phone tour of the Typewriter keyboard.

Runs on an emulator (see .github/workflows/virtual-phone.yml), or on a phone plugged in
over adb:
    python3 tools/virtual-phone/tour.py --apk app-debug.apk --out out

It installs the debug build, switches the phone to the Typewriter keyboard, taps real
keys and checks what was typed, and saves a screenshot of every step to --out with a
report.md. Exits with 1 if any check fails.

The emulator has no sound or vibration: judge those on a real phone.
"""
import argparse
import sys
from pathlib import Path

from phone import IME, Phone

LABELS = ("Type here", "Email", "Search")


class Tour:
    def __init__(self, phone: Phone):
        self.phone = phone
        self.checks: list[tuple[str, bool, str]] = []

    def check(self, name: str, ok: bool, detail: str = "") -> bool:
        self.checks.append((name, bool(ok), detail))
        print(("PASS  " if ok else "FAIL  ") + name + (f"  [{detail}]" if detail else ""), flush=True)
        return bool(ok)

    def text(self, box: int) -> str:
        return self.phone.box_text(box, LABELS[box])

    def run(self, apk: str) -> None:
        p = self.phone
        p.prepare()

        ok, output = p.install(apk)
        if not self.check("The app installs", ok, "" if ok else output.strip()[-300:]):
            return
        self.check("The app has no internet permission",
                   not p.has_permission("android.permission.INTERNET"))

        current = p.use_typewriter()
        self.check("The phone switches to the Typewriter keyboard", current == IME, current)

        p.open_app()
        seen = p.find_soon("Typewriter is your keyboard")
        p.screenshot("welcome")
        self.check("The welcome screen sees the keyboard switched on and picked", seen is not None)

        boxes = p.text_boxes()
        if not self.check("The welcome screen has three boxes to try", len(boxes) == 3,
                          f"found {len(boxes)}"):
            return

        p.clear_log()
        p.tap(boxes[0]["x"], boxes[0]["y"])
        keys = p.keyboard_keys()
        if not self.check("The keyboard opens and reports its 32 keys", len(keys) >= 32,
                          f"{len(keys)} keys"):
            p.screenshot("no-keyboard")
            return
        p.screenshot("keyboard")

        # 1. Typing, with the capital at the start of the sentence added automatically.
        p.type_keys(keys, list("hello") + ["space"] + list("world"))
        self.check('Typing "hello world" gives "Hello world" (automatic capital)',
                   self.text(0) == "Hello world", repr(self.text(0)))
        p.screenshot("typed")

        # 2. Holding a key lights its typebar and stamps the letter on the paper;
        #    letting go types it, and delete removes it again.
        p.hold(keys["t"], ms=2500, shoot="held-key")
        self.check("Letting go of a held key types it", self.text(0) == "Hello worldt",
                   repr(self.text(0)))
        p.type_keys(keys, ["delete"])
        self.check("Delete removes one letter", self.text(0) == "Hello world", repr(self.text(0)))

        # 3. One tap on shift gives one capital.
        p.type_keys(keys, ["space", "shift"])
        p.screenshot("shift-on")
        p.type_keys(keys, ["a", "b"])
        self.check("Shift capitalises only the next letter", self.text(0) == "Hello world Ab",
                   repr(self.text(0)))

        # 4. Enter follows what each box asks for: "Next" here...
        p.type_keys(keys, ["enter"] + list("me"))
        self.check("Enter moves on to the email box", self.text(1) == "me", repr(self.text(1)))

        # 5. ...and "Search" in the last box.
        p.type_keys(keys, ["enter"] + list("ink") + ["enter"])
        self.check("Enter in the search box runs the search",
                   p.find_soon("Searched for: ink", seconds=8) is not None)
        p.screenshot("searched")

        # 6. Holding delete keeps deleting.
        p.hold(keys["delete"], ms=1800)
        self.check("Holding delete keeps deleting", self.text(2) == "", repr(self.text(2)))
        p.screenshot("end")

    def report(self, out: Path) -> bool:
        passed = sum(1 for _, ok, _ in self.checks if ok)
        all_ok = bool(self.checks) and passed == len(self.checks)
        lines = [
            "# Virtual phone: Typewriter keyboard",
            "",
            f"**{passed} of {len(self.checks)} checks passed.**",
            "",
            "| Check | Result | Detail |",
            "|---|---|---|",
        ]
        for name, ok, detail in self.checks:
            lines.append(f"| {name} | {'✅' if ok else '❌'} | {detail.replace('|', '/')} |")
        shots = sorted(f.name for f in out.glob("*.png"))
        lines += ["", "## Screenshots", ""] + [f"- {s}" for s in shots]
        lines += ["", "An emulator has no sound or vibration, and it is not a budget phone: "
                      "check the feel on a real phone."]
        (out / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
        return all_ok


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apk", required=True, help="the debug APK to install")
    parser.add_argument("--out", required=True, help="folder for screenshots and report.md")
    args = parser.parse_args()

    out = Path(args.out)
    tour = Tour(Phone(out))
    try:
        tour.run(args.apk)
    except Exception as e:  # report what happened instead of losing the screenshots
        tour.check("The tour ran to the end", False, f"{type(e).__name__}: {e}")
    ok = tour.report(out)
    print((out / "report.md").read_text(encoding="utf-8"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
