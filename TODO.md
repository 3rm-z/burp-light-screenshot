# Burp Light Screenshot — backlog

Ordine approssimativo; spunta o riordina quando serve.

## Bug / affidabilità

- [x] **Freeze UI / riquadro grigio**: cattura + clipboard spostate su thread di lavoro; AWT clipboard con `invokeAndWait` sull’EDT; glass pane ripristinato in `finally`
- [ ] **Clipboard Linux / VM→host**: se `xclip` exit 0 ma l’immagine non arriva su Windows → spesso sync guest solo **testo**; workaround = PNG in `/tmp` o cartella condivisa
- [x] **Clipboard Linux (Kali/i3)**: niente AWT; `xclip -i`; `DISPLAY`/`XAUTHORITY`; clipboard + primary; prova xclip prima se `DISPLAY` settato (ibrido Wayland)
- [x] **Clipboard Windows**: AWT dal worker + fallback PowerShell WinForms (`ClipboardCapture`)
- [ ] **Conferma** che Burp non venga più avviato in modo che `PATH` non includa `/usr/bin` (GUI vs terminale)

## Qualità immagine

- [ ] Affinare ancora filtro report se serve (dopo tuning gamma/saturazione/value)
- [x] Opzione menu **screenshot senza filtro** (tema scuro originale 1:1) per confronto / report dove va bene il dark

## UX

- [x] **Colore bordo selezione** personalizzabile via `-Dburp.lightss.selection.color=...` (vedi README)
- [x] **Preset colore bordo** da menu Burp (`Selection border color`) + reset a property/default
- [ ] UI in-estensione (tab) per colore bordo senza modificare lo script di avvio Burp
- [ ] Feedback **modalità screenshot** (cursore crosshair già possibile sul glass pane; messaggio status bar / dialog leggero “Seleziona regione…”)
- [ ] **Conferma visiva** a fine capture (toast / riga Output già presente — eventualmente dialog non modale “Copiato + path file”)
- [x] **Esc** per uscire dalla selezione senza rettangolo

## Documentazione

- [x] README: build, bordo, Linux, **come quantificare** output filtro
- [ ] Nota VM: clipboard immagini host/guest

---

*Ultimo aggiornamento: manutenzione freeze EDT / overlay.*
