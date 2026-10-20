package ir;

import java.io.IOException;
import java.util.List;

/** Prova da riga di comando: java -cp target/classes ir.Cli "query" [--no-fuzzy] [--ocr] [--file] [--tfidf|--bm25] [--or]
 *  --file = indice persistente di ir.Persistenza; --tfidf/--bm25 = ordina per punteggio; --or = almeno una parola (default: tutte)
 *  Ogni riga e' seguita, fra parentesi quadre, dai termini del dizionario che l'hanno fatta trovare; con --tfidf/--bm25
 *  e' preceduta dal punteggio. */
public final class Cli {
    public static void main(String[] args) throws IOException {
        List<String> opz = List.of(args).subList(1, args.length);
        Indice ix = Persistenza.apri(opz.contains("--ocr"), opz.contains("--file"));
        Searcher s = new Searcher(ix, !opz.contains("--no-fuzzy"));
        Ranker.Modello m = opz.contains("--bm25") ? Ranker.Modello.BM25 : opz.contains("--tfidf") ? Ranker.Modello.TFIDF : Ranker.Modello.NESSUNO;
        Searcher.Risposta r = s.searchDettaglio(args[0], m, !opz.contains("--or"), 10);
        System.out.println(r.totale() + " risultati");
        for (Searcher.Risultato x : r.risultati()) {
            String punteggio = m == Ranker.Modello.NESSUNO ? "" : String.format("%.3f  ", x.punteggio());
            System.out.println("  " + punteggio + ix.doc(x.docId()) + "  [" + String.join(", ", x.termini()) + "]");
        }
    }
}
