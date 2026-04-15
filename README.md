# Burp Light Screenshot Helper

Estensione Burp (Montoya): screenshot di una regione della finestra, stile “report chiaro” oppure colori originali, clipboard (+ PNG in `/tmp` su Linux).

## Build

```bash
./gradlew jar
```

Carica il JAR generato in **Extensions**.

## Menu in Burp

Nel menu `Light Screenshot` trovi:

- `Select region -> Clipboard (report / chiaro)` → applica il filtro colore
- `Select region -> Clipboard (original colors)` → nessun filtro (1:1 rispetto allo schermo)
- voci `[Border] ...` → preset rapidi (neon) + reset

## Personalizzazione bordo selezione

Puoi cambiare il colore in due modi:

1. **Da menu Burp**: voci `Light Screenshot -> [Border] ...`
2. **Da JVM property** (default all'avvio), ad esempio:

```text
-Dburp.lightss.selection.color=#00E5FFCC
```

Formati:

| Formato | Esempio |
|--------|---------|
| `#RRGGBB` | `#FF6600` |
| `#RRGGBBAA` | `#FF6600D0` (alpha in hex) |
| `r,g,b[,a]` | `0,200,255,200` |

Proprietà: `burp.lightss.selection.color` (vedi `SelectionAppearance`).  
Voce menu utile: `Reset to JVM property/default`.

## Come “quantificare” se l’immagine è troppo ombrata / spenta

Obiettivo: dare un feedback **ripetibile** senza parole vaghe.

1. **Confronto visivo affiancato**  
   Salva due PNG della stessa regione (es. raw dark da altro tool vs output dell’estensione) e mettile affiancate in un viewer. È il riferimento più diretto.

2. **Luminosità media (ImageMagick)**  
   ```bash
   identify -verbose screenshot.png | grep -i mean
   ```  
   Valori **mean** più bassi → immagine globalmente più scura (“ombra”).

3. **Contrasto / deviazione**  
   In GIMP: **Colori → Informazioni → Istogramma** sulla luminosità; se tutto è ammassato al centro → “piatta / ombrata”.

4. **Numeri che puoi incollarmi**  
   - Media RGB approssimata (es. “mean ~180,180,185”)  
   - O screenshot di istogramma  
   - “Header azzurro e testo attributo hanno quasi stesso grigio” (cosa specifica + possibilmente crop zoom)

5. **In codice** (se un giorno servisse)  
   Si può aggiungere un log opzionale: luminanza media dell’immagine filtrata, o export side-by-side raw/filtrato.

## Linux (Kali, i3, X11)

L’estensione prova **`xclip`** in più modi (senza/s con `-i`, ordine argomenti, `cat | xclip` via `/bin/sh`) e, se tutto fallisce, **fallback AWT** (`sudo apt install xclip`).

- In **Output** compaiono `DISPLAY=…` e l’esito di `xclip`; se `DISPLAY` è “non impostato”, avvia Burp dallo stesso contesto dove hai X11 (es. da terminale dentro la sessione grafica).
- Viene copiata anche la selection **`primary`** (incolla con **tasto centrale** in alcune app).
- **VM → Windows**: molti hypervisor sincronizzano la clipboard **solo testo** verso l’host. Se su Kali `xclip` ha exit 0 ma su Windows non vedi l’immagine, è spesso un limite del guest: usa il **file PNG** in `/tmp` o una cartella condivisa.

## Windows

Se l’incolla immagine non funziona solo con AWT, l’estensione prova anche **PowerShell** (`powershell.exe -Sta`) con `System.Windows.Forms.Clipboard::SetImage` sul PNG in `%TEMP%`. Serve che PowerShell sia nel `PATH` (normale su Windows 10/11).

## TODO

Vedi `TODO.md` nel repo.
