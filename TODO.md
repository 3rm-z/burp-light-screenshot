# Burp Light Screenshot — backlog

Ordine approssimativo; spunta o riordina quando serve.

## Bug / affidabilità

- [x] **Freeze UI / riquadro grigio**: cattura + clipboard spostate su thread di lavoro; AWT clipboard con `invokeAndWait` sull’EDT; glass pane ripristinato in `finally`
- [ ] **Clipboard Linux**: ancora non incolla in alcuni setup — indagare `DISPLAY`, sandbox Flatpak, `xclip` vs `xsel`, test con `xclip -o -selection clipboard -t TARGETS` dopo copia
- [ ] **Conferma** che Burp non venga più avviato in modo che `PATH` non includa `/usr/bin` (GUI vs terminale)

## Qualità immagine

- [ ] Ridurre effetto “sotto ombra” / grigiastro nel filtro report (gamma, saturazione leggera, o curva separata per mid-tones)
- [ ] Opzione menu **screenshot senza filtro** (tema scuro originale 1:1) per confronto / report dove va bene il dark

## UX

- [ ] Feedback **modalità screenshot** (cursore crosshair già possibile sul glass pane; messaggio status bar / dialog leggero “Seleziona regione…”)
- [ ] **Conferma visiva** a fine capture (toast / riga Output già presente — eventualmente dialog non modale “Copiato + path file”)
- [x] **Esc** per uscire dalla selezione senza rettangolo

## Documentazione

- [ ] README: prerequisiti Kali (`xclip`, Wayland → `wl-copy`), nota VM clipboard immagini

---

*Ultimo aggiornamento: manutenzione freeze EDT / overlay.*
