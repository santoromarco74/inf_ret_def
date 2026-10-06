# Riproduzione

```
scripts/ocr.sh                                          # scansioni/ -> data/ocr/  (richiede tesseract-ocr-ita, imagemagick, poppler-utils)
mvn -q compile
java -cp target/classes ir.corpus.CorpusBuilder         # data/ocr -> data/corpus.tsv, data/righe_escluse.tsv
java -cp target/classes ir.Cli "friggitrice aria" --bm25 --or   # ordine per punteggio (--tfidf, --bm25), --or = almeno una parola
java -cp target/classes ir.Cli "lava*" --ocr            # prova da riga di comando
java -cp target/classes ir.WebServer 8080               # interfaccia web su http://localhost:8080
java -cp target/classes ir.Persistenza                  # salva l'indice compresso: data/indice.bin e data/indice_ocr.bin
java -cp target/classes ir.Persistenza tempi carica 20  # tempo di avvio da file (oppure: tempi build 20)
java -cp target/classes ir.Cli "lava*" --file           # ricerca sull'indice caricato da file
java -cp target/classes ir.WebServer 8080 --file        # interfaccia web sull'indice caricato da file
java -cp target/classes ir.Esempi                       # gli esempi citati nella relazione
java -cp target/classes ir.Compressione                 # spazio prima/dopo la compressione
java -cp target/classes ir.ValutaCorrezione             # valutazione della correzione OCR
java -cp target/classes ir.ConfrontoStrutture           # tabella hash, albero binario di ricerca, heap: statistiche e tempi (data/confronto_strutture.txt)
java -cp target/classes ir.Benchmark                   # data/risultati_benchmark.txt
python3 scripts/grafici.py                              # docs/img/*.svg dai risultati del benchmark
mvn -q test                                             # test automatici
```

Le scansioni sono dati aziendali: la pipeline le legge da `scansioni/`; il lavoro prodotto (script, testo OCR, corpus estratto) sta in `scripts/` e `data/`.

## Windows (PowerShell)

Serve un JDK 17 o superiore a 64 bit (`java -version`); Maven non va installato, c'è il wrapper `mvnw.cmd` (al primo avvio scarica Maven). Il passo OCR (`scripts/ocr.sh`) è uno script bash: su Windows non serve rilanciarlo, perché il testo OCR è già in `data/ocr/`.

```
.\mvnw.cmd -q compile

java -cp target\classes ir.corpus.CorpusBuilder      # data\ocr -> data\corpus.tsv, data\righe_escluse.tsv
java -cp target\classes ir.Cli "friggitrice aria" --bm25 --or   # ordine per punteggio
java -cp target\classes ir.Cli "lava*" --ocr          # prova da riga di comando
java -cp target\classes ir.WebServer 8080             # interfaccia web su http://localhost:8080
java -cp target\classes ir.Persistenza                 # salva data\indice.bin e data\indice_ocr.bin
java -cp target\classes ir.Cli "lava*" --file          # ricerca sull'indice caricato da file
java -cp target\classes ir.WebServer 8080 --file       # interfaccia web sull'indice caricato da file
java -cp target\classes ir.Persistenza tempi carica 20
java -cp target\classes ir.Esempi
java -cp target\classes ir.Compressione
java -cp target\classes ir.ValutaCorrezione
java -cp target\classes ir.ConfrontoStrutture
java -cp target\classes ir.Benchmark
.\mvnw.cmd -q test
```
