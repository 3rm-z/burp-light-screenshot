# Release Checklist

Use this checklist before publishing a new release or submitting to Burp BApp Store.

## 1) Versioning and Metadata

- [ ] Bump `version` in `build.gradle`
- [ ] Ensure extension name in `BurpExtender` is final and user-friendly
- [ ] Review menu labels and log wording (public-facing, no internal notes)

## 2) Build

- [ ] Build JAR locally:
  - `gradle clean jar`
- [ ] Confirm JAR is generated and loadable in Burp
- [ ] Verify no unexpected runtime dependency issues

## 3) Smoke Test (Primary OS)

- [ ] Select a region and capture to clipboard
- [ ] Test both modes: `Report / light` and `Original colors`
- [ ] Confirm border color presets and `Reset` work
- [ ] Confirm `Esc` cancels selection cleanly
- [ ] Confirm temporary PNG is created

## 4) Clipboard Reliability

- [ ] Linux: verify `xclip` path works
- [ ] Linux: verify fallback path behavior if native clipboard fails
- [ ] Windows (optional but recommended): verify AWT clipboard and PowerShell fallback

## 5) Docs and Repo Hygiene

- [ ] README reflects current behavior and build command
- [ ] TODO/backlog is up to date
- [ ] Remove accidental debug or local-only references
- [ ] Check comments/logs are in English

## 6) Pre-Submission (BApp Store)

- [ ] Prepare short extension description
- [ ] Prepare usage notes and known limitations
- [ ] Prepare source repository URL and license
- [ ] Validate compatibility with current Montoya API/Burp version
- [ ] Publish release artifact and submit through official BApp process
