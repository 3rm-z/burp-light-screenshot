# Burp Light Screenshot — backlog

Ordine approssimativo; spunta o riordina quando serve.

## Bug / affidabilità

- [x] **Freeze UI / riquadro grigio**: cattura + clipboard spostate su thread di lavoro; AWT clipboard con `invokeAndWait` sull’EDT; glass pane ripristinato in `finally`
- [ ] **Clipboard Linux**: ancora non incolla in alcuni setup — indagare `DISPLAY`, sandbox Flatpak, `xclip` vs `xsel`, test con `xclip -o -selection clipboard -t TARGETS` dopo copia
- [x] **Clipboard Windows**: AWT dal worker + fallback PowerShell WinForms (`ClipboardCapture`)
- [ ] **Conferma** che Burp non venga più avviato in modo che `PATH` non includa `/usr/bin` (GUI vs terminale)

## Qualità immagine

- [ ] Affinare ancora filtro report se serve (dopo tuning gamma/saturazione/value)
- [ ] Opzione menu **screenshot senza filtro** (tema scuro originale 1:1) per confronto / report dove va bene il dark

## UX

- [x] **Colore bordo selezione** personalizzabile via `-Dburp.lightss.selection.color=...` (vedi README)
- [ ] UI in-estensione (tab) per colore bordo senza modificare lo script di avvio Burp
- [ ] Feedback **modalità screenshot** (cursore crosshair già possibile sul glass pane; messaggio status bar / dialog leggero “Seleziona regione…”)
- [ ] **Conferma visiva** a fine capture (toast / riga Output già presente — eventualmente dialog non modale “Copiato + path file”)
- [x] **Esc** per uscire dalla selezione senza rettangolo

## Documentazione

- [x] README: build, bordo, Linux, **come quantificare** output filtro
- [ ] Nota VM: clipboard immagini host/guest

---

*Ultimo aggiornamento: manutenzione freeze EDT / overlay.*
