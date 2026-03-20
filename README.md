# Burp Light Screenshot Helper

Estensione Burp (Montoya): screenshot di una regione della finestra, stile “report chiaro”, clipboard (+ PNG in `/tmp` su Linux).

## Build

```bash
./gradlew jar
```

Carica il JAR generato in **Extensions**.

## Personalizzazione bordo selezione

All’avvio della JVM di Burp (script di lancio, `.desktop`, ecc.) aggiungi una **system property**:

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

## Linux

Utile avere `xclip` (X11) o `wl-copy` (Wayland) per la clipboard immagine.

## TODO

Vedi `TODO.md` nel repo.
