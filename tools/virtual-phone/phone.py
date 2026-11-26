"""Phone: small helpers that drive an Android emulator over adb, used by the tour.

Only adb and the Python standard library:
  - `ime enable` / `ime set` switch the emulator to the Typewriter keyboard;
  - the debug build logs where each key sits on screen (logcat tag TWKEYS, positions only,
    never anything typed), so the tour taps real keys;
  - `uiautomator dump` lists what is on screen (Compose exposes its texts and text boxes);
  - `screencap` saves a picture of the screen.

Adapted from the Linda app's virtual phone.
"""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.lester.typewriter"
IME = f"{PKG}/.features.keyboard.TypewriterIME"
ACTIVITY = f"{PKG}/.features.onboarding.MainActivity"
KEY_LOG_TAG = "TWKEYS"
_KEY_LINE = re.compile(r"key (\S+) (-?\d+) (-?\d+)")
_BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def parse_key_log(text: str) -> dict[str, tuple[int, int]]:
    """The latest on-screen centre of each key, from `logcat -s TWKEYS` output."""
    keys: dict[str, tuple[int, int]] = {}
    for line in text.splitlines():
        m = _KEY_LINE.search(line)
        if m:
            keys[m.group(1)] = (int(m.group(2)), int(m.group(3)))
    return keys


def parse_nodes(xml: str) -> list[dict]:
    """Every on-screen element from a `uiautomator dump`, with its text, class, focus and centre."""
    start = xml.find("<?xml")
    if start < 0:
        return []
    try:
        root = ET.fromstring(xml[start:xml.rfind(">") + 1])
    except ET.ParseError:
        return []
    nodes = []
    for n in root.iter("node"):
        m = _BOUNDS.match(n.get("bounds") or "")
        if not m:
            continue
        x1, y1, x2, y2 = map(int, m.groups())
        nodes.append({
            "text": (n.get("text") or "").strip(),
            "desc": (n.get("content-desc") or "").strip(),
            "cls": n.get("class") or "",
            "focused": n.get("focused") == "true",
            "x": (x1 + x2) // 2,
            "y": (y1 + y2) // 2,
            "top": y1,
        })
    return nodes


def box_value(text: str, label: str) -> str:
    """A text box's value. Some Android versions report the label with it ("Search" or "Search, ink")."""
    if text == label:
        return ""
    for sep in (", ", ","):
        if text.startswith(label + sep):
            return text[len(label) + len(sep):]
    return text


class Phone:
    def __init__(self, out: Path):
        self.out = out
        self.out.mkdir(parents=True, exist_ok=True)
        self.shots = 0

    # ---------- adb ----------
    def adb(self, *args: str, timeout: int = 60) -> str:
        p = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)
        return (p.stdout or "") + (p.stderr or "")

    def shell(self, cmd: str, timeout: int = 60) -> str:
        return self.adb("shell", cmd, timeout=timeout)

    def prepare(self) -> None:
        """Wait for the emulator to finish booting, then wake and unlock it."""
        self.adb("wait-for-device", timeout=300)
        self.wait_for(lambda: self.shell("getprop sys.boot_completed").strip() == "1", 300, 3)
        for scale in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            self.shell(f"settings put global {scale} 0")
        self.shell("input keyevent KEYCODE_WAKEUP")
        self.shell("wm dismiss-keyguard")
        time.sleep(2)

    def install(self, apk: str) -> tuple[bool, str]:
        self.adb("uninstall", PKG)
        out = self.adb("install", "-r", apk, timeout=180)
        return "Success" in out, out

    def use_typewriter(self) -> str:
        """Switch the phone to the Typewriter keyboard (a person does this once in Settings)."""
        self.shell(f"ime enable {IME}")
        self.shell(f"ime set {IME}")
        return self.shell("settings get secure default_input_method").strip()

    def open_app(self) -> None:
        self.shell(f"am start -W -n {ACTIVITY}", timeout=90)
        time.sleep(3)

    def has_permission(self, permission: str) -> bool:
        return permission in self.shell(f"dumpsys package {PKG}")

    # ---------- the screen ----------
    def screenshot(self, name: str) -> str:
        self.shots += 1
        fname = f"{self.shots:02d}-{name}.png"
        with open(self.out / fname, "wb") as f:
            f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"],
                                   capture_output=True, timeout=60).stdout)
        return fname

    def nodes(self) -> list[dict]:
        self.shell("uiautomator dump /sdcard/ui.xml", timeout=90)
        return parse_nodes(self.adb("exec-out", "cat", "/sdcard/ui.xml"))

    def find(self, words: str) -> dict | None:
        for n in self.nodes():
            if words.lower() in (n["text"] + " " + n["desc"]).lower():
                return n
        return None

    def text_boxes(self) -> list[dict]:
        """The app's text boxes, top to bottom."""
        boxes = [n for n in self.nodes() if n["cls"].endswith("EditText")]
        return sorted(boxes, key=lambda n: n["top"])

    def box_text(self, index: int, label: str) -> str:
        boxes = self.text_boxes()
        return box_value(boxes[index]["text"], label) if index < len(boxes) else "<missing>"

    # ---------- touching ----------
    def tap(self, x: int, y: int) -> None:
        self.shell(f"input tap {x} {y}")
        time.sleep(0.25)

    def type_keys(self, keys: dict[str, tuple[int, int]], ids: list[str]) -> None:
        """Tap keyboard keys by their ids ("h", "space", "shift", "enter"...)."""
        for key_id in ids:
            x, y = keys[key_id]
            self.tap(x, y)
        time.sleep(1.0)  # let the app catch up before anyone reads the text

    def hold(self, at: tuple[int, int], ms: int, shoot: str | None = None) -> None:
        """Press and hold one spot for ms milliseconds, optionally taking a screenshot halfway."""
        x, y = at
        finger = subprocess.Popen(["adb", "shell", f"input swipe {x} {y} {x} {y} {ms}"])
        if shoot:
            time.sleep(ms / 2000)
            self.screenshot(shoot)
        finger.wait(timeout=60)
        time.sleep(1.0)

    # ---------- the keyboard ----------
    def clear_log(self) -> None:
        self.adb("logcat", "-c")

    def keyboard_keys(self, seconds: float = 20) -> dict[str, tuple[int, int]]:
        """Wait for the keyboard to show and log where its keys are."""
        found: dict[str, tuple[int, int]] = {}

        def ready() -> bool:
            nonlocal found
            found = parse_key_log(self.adb("logcat", "-d", "-s", f"{KEY_LOG_TAG}:D"))
            return len(found) >= 30

        self.wait_for(ready, seconds, 1.0)
        time.sleep(1.0)
        # Read once more: the last report is the one taken after the keyboard settled.
        found = parse_key_log(self.adb("logcat", "-d", "-s", f"{KEY_LOG_TAG}:D")) or found
        return found

    # ---------- waiting ----------
    def wait_for(self, fn, seconds: float = 20, every: float = 1.0) -> bool:
        end = time.time() + seconds
        while time.time() < end:
            if fn():
                return True
            time.sleep(every)
        return False
