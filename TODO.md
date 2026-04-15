# Burp Light Screenshot - Backlog

Rough priority order; reorder as needed.

## Bugs / Reliability

- [x] **UI freeze / gray overlay**: moved capture + clipboard to worker thread, EDT `invokeAndWait` for AWT clipboard, always restore glass pane in `finally`
- [ ] **Linux clipboard / guest-host sync**: if `xclip` exits 0 but image is not visible on host, document VM sync limitations and fallback workflow
- [x] **Linux clipboard robustness**: no AWT-by-default path, `xclip -i` variants, `DISPLAY`/`XAUTHORITY`, both clipboard + primary selections
- [x] **Windows clipboard robustness**: worker-thread AWT + PowerShell WinForms fallback (`ClipboardCapture`)
- [ ] **Launch context check**: verify Burp startup environment always includes required clipboard tools on Linux

## Image Quality

- [ ] Further tune report filter if needed (gamma/saturation/value)
- [x] Menu option for **unfiltered screenshot** (original dark colors 1:1)

## UX

- [x] Custom **selection border color** via `-Dburp.lightss.selection.color=...` (see README)
- [x] Burp menu **border presets** + reset to property/default
- [ ] Optional in-extension settings tab for border color (no startup script edits required)
- [ ] Clearer **capture mode feedback** (status text or lightweight prompt before selection)
- [ ] Optional **capture completion feedback** (toast or non-modal confirmation)
- [x] **Esc** cancels selection

## Documentation

- [x] README: build, border options, Linux/Windows clipboard behavior
- [ ] Add a short release checklist for packaging and store submission
