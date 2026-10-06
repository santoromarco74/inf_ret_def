# Archivio Bolle

Ricerca di articoli su bolle/DDT scansionati (testo OCR) con indici sviluppati nel progetto.

## Tabella «codice / libreria»

| Componente | Codice / libreria | Stato |
|---|---|---|
| Tokenizer | codice | fatto |
| Indice invertito (postings, ricerca AND) | codice | fatto |
| Dizionario: tabella hash con liste di collisione e albero binario di ricerca per ordine e prefissi (`TabellaHash`, `AlberoBinarioRicerca`); heap binario per i primi k risultati (`HeapBinario`); misure in `ir.ConfrontoStrutture` | codice (nessuna collezione di `java.util` nelle strutture) | fatto |
| Skip list sui postings (`PostingList`) | codice | fatto |
| Indice a trigrammi sui termini + fuzzy (Jaccard + Levenshtein, `KGramIndex`, `EditDistance`) | codice | fatto |
| Wildcard (stesso indice a k-grammi, `*` ovunque) | codice | fatto |
| Compressione dei postings: gap + VByte; dizionario non compresso con ricerca binaria (`VByte`, `DizionarioOrdinato`, `CompressedIndex`) | codice | fatto |
| Indice persistente su file (`CompressedIndex.salva/carica`, `Persistenza`, interfaccia `Indice`) | codice (`java.io` della JDK) | fatto |
| Ranking TF-IDF (lnc.ltn) e BM25 (`Ranker`, `Searcher.searchRanked`), valutato con MAP, P@1, R-precision | codice | fatto |
| Correzione OCR mirata sui termini (scambi confusabili 0/o 1/i 5/s 6/g 8/b 7/t verso termini con df maggiore, `OcrCorrector`) | codice | fatto |
| OCR (Tesseract `ita`, rotazione via OSD; `scripts/ocr.sh`) | libreria/strumento esterno | fatto: 134 scansioni → `data/ocr/` |
| Parsing OCR → `data/corpus.tsv` + righe escluse tracciate (`ir.corpus`, regex JDK) | codice | fatto |
| Interfaccia web minimale (barra di ricerca + 2 opzioni, `com.sun.net.httpserver` della JDK, `WebServer`) | codice sopra libreria standard | fatto |
| Test collection (ricerca dell'articolo noto, rilevanza per codice) e metriche P/R/F1/accuratezza (`Benchmark`) | codice | fatto: `data/risultati_benchmark.txt` |
| Esempi riproducibili per la relazione (`Esempi`) e grafici SVG (`scripts/grafici.py`, Python stdlib) | codice | fatto |
| fastText come modello di confronto (`FastTextConfronto`, wrapper `com.github.vinhkhuc:jfasttext`) | **libreria** | fatto, solo nel benchmark |

## Come eseguire

Vedi `docs/RIPRODUZIONE.md` (Linux/macOS e Windows PowerShell).
"# inf_ret_def" 
