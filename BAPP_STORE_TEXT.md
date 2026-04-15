# BApp Store Submission Text

## Name
Burp Light Screenshot

## Short Description
Take light-mode screenshots while working in Burp dark mode, with direct clipboard copy and original-color fallback.

## Long Description
Burp Light Screenshot helps you capture report-ready screenshots without leaving your preferred dark theme workflow.

Select any region inside Burp and choose between:

- **Report / light**: converts dark UI captures into a light, document-friendly style.
- **Original colors**: keeps source colors unchanged.

The extension focuses on practical reporting speed:

- Region selection overlay with Esc-to-cancel
- Clipboard copy after each capture
- Border color presets
- Linux clipboard reliability paths (`xclip`, `wl-copy`)
- Windows clipboard fallback via PowerShell
- Temporary PNG output for manual reuse/troubleshooting

## Known Limitations
- Host/guest clipboard behavior can differ in VM setups.
- Linux reliability depends on available native clipboard tools and display session.
- Report/light conversion is heuristic-based and may vary with source contrast/theme details.
- Captures are tied to the active Burp UI context/window; some task-specific result tabs
  (for example certain Intruder views) may need to be captured from persistent Dashboard/history views instead.
