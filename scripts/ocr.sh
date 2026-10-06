#!/usr/bin/env bash
# Passo 1 della pipeline: scansione -> testo grezzo. Strumenti esterni (Tesseract lingua ita,
# ImageMagick, poppler), non parte del codice del progetto. L'orientamento di ogni pagina si rileva con l'OSD di Tesseract:
# parte delle scansioni e' capovolta di 180 gradi.
# Uso: scripts/ocr.sh [cartella_scansioni] [cartella_output]
set -euo pipefail
IN="${1:-scansioni}"
OUT="${2:-data/ocr}"
mkdir -p "$OUT"

ocr_file() {
  f="$1"; OUT="$2"
  base="$(basename "${f%.*}")"
  tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  case "$f" in
    *.pdf) pdftoppm -r 300 -png "$f" "$tmp/p" ;;
    *)     convert "$f" "$tmp/p-%03d.png" ;;
  esac
  : > "$OUT/$base.txt"
  for p in "$tmp"/p-*.png; do
    rot="$(tesseract "$p" stdout --psm 0 -l osd 2>/dev/null | awk '/^Rotate:/{print $2}')"
    [ "${rot:-0}" != "0" ] && convert "$p" -rotate "$rot" "$p"
    tesseract "$p" stdout -l ita --psm 6 >> "$OUT/$base.txt" 2>/dev/null
    printf '\f' >> "$OUT/$base.txt"   # separatore di pagina
  done
  echo "ok $base"
}
export -f ocr_file
export OMP_THREAD_LIMIT=1   # un thread per processo: con piu processi in parallelo il default si blocca

ls "$IN"/*.tif "$IN"/*.tiff "$IN"/*.pdf 2>/dev/null | xargs -P "$(nproc)" -I{} bash -c 'ocr_file "$@"' _ {} "$OUT"
