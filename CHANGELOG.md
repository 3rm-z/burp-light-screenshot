# Changelog

All notable changes to this project are documented here.

## [1.0.1] - 2026-04-15

### Fixed
- Reduced pixelated/grainy appearance in `Report / light` mode by switching sharpening to edge-aware behavior.
- Lowered sharpening intensity and skipped flat regions to preserve text clarity without adding noise.

## [1.0.0] - 2026-04-15

### Added
- Region screenshot capture with glass-pane overlay selection.
- Color modes: `Report / light` and `Original colors`.
- Selection border color presets with runtime reset.
- Linux clipboard integration (`xclip` / `wl-copy`) with fallback behavior.
- Windows clipboard fallback via PowerShell WinForms.
- Temporary PNG output for troubleshooting and manual reuse.
- Release checklist and repository docs in English.

### Notes
- Core value: create light-mode screenshots while continuing to use Burp in dark mode.
- Known limitation documented: capture flow is tied to active Burp UI context (some task-specific tabs may require dashboard/history workaround).
