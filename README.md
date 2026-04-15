# Burp Light Screenshot

Burp (Montoya) extension for region screenshots with two output modes:

- `Report / light`: generates light-mode screenshots while you keep working in dark mode.
- `Original colors`: keeps source colors unchanged.

Each capture is copied to clipboard and also saved as a temporary PNG.

## Build

```bash
gradle jar
```

Load the generated JAR in Burp `Extensions`.

## Install (GitHub Release)

- Download the latest JAR from the GitHub Releases page.
- In Burp, open `Extensions` -> `Installed` -> `Add`.
- Select type `Java`, then load the downloaded JAR.

## Burp Menu

The `Light Screenshot` menu includes:

- `Take screenshot -> Clipboard`: starts region selection with the active color mode.
- `Color mode`: exclusive choice between `Report / light` and `Original colors`.
- `Border color`: quick border presets plus `Reset`.

## Border Color Configuration

You can set the selection border color in two ways:

1. From Burp menu: `Light Screenshot -> Border color`
2. Via JVM property (startup default), for example:

```text
-Dburp.lightss.selection.color=#00E5FFCC
```

Supported formats:

| Format | Example |
|--------|---------|
| `#RRGGBB` | `#FF6600` |
| `#RRGGBBAA` | `#FF6600D0` (hex alpha) |
| `r,g,b[,a]` | `0,200,255,200` |

Property name: `burp.lightss.selection.color` (see `SelectionAppearance`).  
Menu `Reset` restores JVM-property/default value.

## Linux Notes

On Linux, the extension tries native clipboard tools first:

- `xclip` (multiple invocation variants for better compatibility)
- `wl-copy` when Wayland is available
- AWT clipboard fallback if native tooling fails

Logs include `DISPLAY`, `WAYLAND_DISPLAY`, and command outcomes to simplify troubleshooting.

## Windows Notes

If AWT image paste fails, the extension also attempts clipboard set via
PowerShell (`powershell.exe -Sta`) using `System.Windows.Forms.Clipboard::SetImage`.

## Backlog

See `TODO.md`.

## Release

See `RELEASE_CHECKLIST.md` before packaging/submission.

## BApp Submission

See `BAPP_STORE_TEXT.md` for ready-to-use store copy.
