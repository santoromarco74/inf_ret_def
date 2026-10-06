#!/usr/bin/env bash
# Passo 1 della pipeline: scansione -> testo grezzo. Strumenti esterni (Tesseract lingua ita,
# ImageMagick, poppler), non parte del codice del progetto. L'orientamento di ogni pagina si rileva con l'OSD di Tesseract:
# parte delle scansioni e' capovolta di 180 gradi.
# Uso: scripts/ocr.sh [cartella_scansioni] [cartella_output]
# Feedback: errori su stderr, "ok <file>" per ogni file riuscito, riepilogo finale; esce con codice 1 se qualcosa fallisce.
set -euo pipefail
IN="${1:-scansioni}"
OUT="${2:-data/ocr}"

errore() { echo "ERRORE: $*" >&2; }

# Controlli preliminari: una dipendenza mancante va segnalata, non produce file vuoti.
manca=0
for cmd in tesseract pdftoppm; do
  command -v "$cmd" >/dev/null 2>&1 || { errore "comando '$cmd' non trovato (servono tesseract-ocr-ita, imagemagick, poppler-utils)"; manca=1; }
done
# ImageMagick: 'magick' (v7) oppure 'convert' (v6). Su Windows 'convert' di solito e' un altro programma
# (convert.exe di sistema, convertitore di dischi), quindi si accetta solo un comando che si dichiara ImageMagick.
IM=""
for cmd in magick convert; do
  if command -v "$cmd" >/dev/null 2>&1 && "$cmd" -version 2>&1 | grep -qi imagemagick; then IM="$cmd"; break; fi
done
[ -n "$IM" ] || { errore "ImageMagick non trovato: serve 'magick' (v7) o 'convert' (v6) di ImageMagick; su Windows 'convert' e' un programma di sistema diverso"; manca=1; }
export IM
if command -v tesseract >/dev/null 2>&1; then
  langs="$(tesseract --list-langs 2>&1 || true)"
  for l in ita osd; do
    grep -qx "$l" <<<"$langs" || { errore "dati lingua Tesseract '$l' non installati"; manca=1; }
  done
fi
[ "$manca" -eq 0 ] || exit 1
[ -d "$IN" ] || { errore "cartella scansioni '$IN' inesistente"; exit 1; }

trova() { find "$IN" -maxdepth 1 -type f \( -iname '*.tif' -o -iname '*.tiff' -o -iname '*.pdf' \) "$@"; }
N=$(( $(trova | wc -l) ))
[ "$N" -gt 0 ] || { errore "nessun file .tif/.tiff/.pdf in '$IN'"; exit 1; }
mkdir -p "$OUT"

ERR="$(mktemp)"; trap 'rm -f "$ERR"' EXIT   # un nome per riga: file falliti dai processi paralleli
export ERR

fallito() { echo "ERRORE $1: $2" >&2; echo "$1" >> "$ERR"; }

ocr_file() {
  f="$1"; OUT="$2"
  base="$(basename "${f%.*}")"
  tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  case "$f" in
    *.[pP][dD][fF]) pdftoppm -r 300 -png "$f" "$tmp/p" 2>"$tmp/err" ||
                      { fallito "$base" "pdftoppm: $(tail -n1 "$tmp/err")"; return 1; } ;;
    *)              "$IM" "$f" "$tmp/p-%03d.png" 2>"$tmp/err" ||
                      { fallito "$base" "$IM: $(tail -n1 "$tmp/err")"; return 1; } ;;
  esac
  set -- "$tmp"/p-*.png
  [ -e "$1" ] || { fallito "$base" "nessuna pagina estratta"; return 1; }
  : > "$OUT/$base.txt"
  n=0
  for p in "$@"; do
    n=$((n + 1))
    # l'OSD fallisce su pagine con poco testo: e' normale, in quel caso nessuna rotazione
    rot="$(tesseract "$p" stdout --psm 0 -l osd 2>/dev/null | awk '/^Rotate:/{print $2}')"
    [ "${rot:-0}" != "0" ] && "$IM" "$p" -rotate "$rot" "$p"
    tesseract "$p" stdout -l ita --psm 6 >> "$OUT/$base.txt" 2>"$tmp/err" ||
      { fallito "$base" "tesseract, pagina $n: $(tail -n1 "$tmp/err")"; return 1; }
    printf '\f' >> "$OUT/$base.txt"   # separatore di pagina
  done
  grep -q '[^[:space:]]' "$OUT/$base.txt" || { fallito "$base" "nessun testo riconosciuto"; return 1; }
  echo "ok $base ($n pagine)"
}
export -f ocr_file fallito
export OMP_THREAD_LIMIT=1   # un thread per processo: con piu processi in parallelo il default si blocca

P="$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 1)"
echo "OCR di $N file da '$IN' verso '$OUT' ($P processi in parallelo)"
rc=0
trova -print0 | xargs -0 -P "$P" -I{} bash -c 'ocr_file "$@"' _ {} "$OUT" || rc=$?
# xargs esce con 123 quando almeno un file e' fallito (gia' riportato sopra); altri codici = problema di xargs/bash
if [ "$rc" -ne 0 ] && [ "$rc" -ne 123 ]; then errore "xargs terminato con codice $rc"; exit 1; fi

nerr=$(( $(wc -l < "$ERR") ))
echo "Fatto: $((N - nerr)) ok, $nerr con errori su $N file"
[ "$nerr" -eq 0 ] || exit 1
