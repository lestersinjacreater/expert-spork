# Typewriter

An Android keyboard that looks, sounds and feels like a manual typewriter.

It has no internet permission, so nothing you type can ever leave your phone.

## Try the latest build

1. On your phone, open the **latest-build** release of this repository and download
   `typewriter.apk`. Allow your browser to install apps if Android asks.
2. Open **Typewriter**, tap **Open keyboard settings** and switch Typewriter on.
3. Tap **Choose keyboard** and pick Typewriter. Then try it in the boxes on the same screen.

New builds install over the old one and the keyboard stays switched on.

## How it is built

There is no Android Studio step. GitHub Actions builds and tests every push, and a virtual
phone (an emulator on GitHub's servers) types on the keyboard and screenshots it for each
pull request. See [CLAUDE.md](CLAUDE.md) for the rules and the layout of the code.
