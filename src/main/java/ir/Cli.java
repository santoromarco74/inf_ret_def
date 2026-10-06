package ir;

import java.io.IOException;
import java.util.List;

/** Prova da riga di comando: java -cp target/classes ir.Cli "query" [--no-fuzzy] [--ocr] [--file] [--tfidf|--bm25] [--or]
 *  --file = indice persistente di ir.Persistenza; --tfidf/--bm25 = ordina per punteggio; --or = almeno una parola (default: tutte) */
public final class Cli {
    public static void main(String[] args) throws IOException {
        List<String> opz = List.of(args).subList(1, args.length);
        Indice ix = Persistenza.apri(opz.contains("--ocr"), opz.contains("--file"));
        Searcher s = new Searcher(ix, !opz.contains("--no-fuzzy"));
        Ranker.Modello m = opz.contains("--bm25") ? Ranker.Modello.BM25 : opz.contains("--tfidf") ? Ranker.Modello.TFIDF : Ranker.Modello.NESSUNO;
        List<Integer> res = s.searchRanked(args[0], m, !opz.contains("--or"));
        System.out.println(res.size() + " risultati");
        for (int id : res.subList(0, Math.min(10, res.size()))) System.out.println("  " + ix.doc(id));
    }
}
