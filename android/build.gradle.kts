// Plugin versions are pinned here (see CLAUDE.md, "Toolchain"). Do not upgrade
// unless a build fails because of them.
// AGP 8.13 is the newest 8.x line and supports Android 16 (API 36), which Google
// Play requires for new apps.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
