package ir;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Test collection e metriche (complemento al lavoro sulle strutture, non il suo centro).
 *
 * Collezione: ricerca dell'articolo noto. Per ogni codice articolo presente in almeno 2 righe del corpus la query
 * viene dalla lettura piu' frequente della descrizione (modello + prima parola descrittiva, oppure prefisso del
 * modello con '*'); i documenti rilevanti sono TUTTE le righe con quel codice, comprese quelle in cui l'OCR ha
 * letto male modello o parole. Il giudizio di rilevanza e' automatico (codice uguale), non manuale.
 *
 * Uso: java -cp target/classes ir.Benchmark
 */
public final class Benchmark {
    record Query(String testo, Set<Integer> rilevanti, boolean conVarianti) {}

    public static void main(String[] args) throws IOException {
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        InvertedIndex base = new InvertedIndex();
        Map<String, List<Integer>> perCodice = new HashMap<>();
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            int id = base.add(c[4] + " " + c[5]);
            perCodice.computeIfAbsent(c[4], k -> new ArrayList<>()).add(id);
        }
        InvertedIndex corretto = OcrCorrector.applica(base, OcrCorrector.calcola(base));

        List<Query> esatte = new ArrayList<>(), jolly = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : perCodice.entrySet()) {
            if (e.getValue().size() < 2) continue;
            Map<String, Integer> freq = new HashMap<>();
            for (int id : e.getValue()) freq.merge(base.doc(id), 1, Integer::sum);
            String rif = freq.entrySet().stream()
                    .max((x, y) -> x.getValue().equals(y.getValue()) ? x.getKey().compareTo(y.getKey()) : x.getValue() - y.getValue())
                    .get().getKey();
            List<String> t = Tokenizer.tokenize(rif); // t0 = codice, t1 = marca, t2 = modello, t3... descrizione
            if (t.size() < 4) continue;
            String modello = t.get(2), parola = t.get(3);
            if (modello.length() < 5) continue;
            Set<Integer> rilevanti = new HashSet<>(e.getValue());
            // "variante OCR": almeno una riga rilevante non contiene il modello o la parola cosi' come sono nella lettura piu' frequente
            boolean varianti = false;
            for (int id : e.getValue()) {
                List<String> td = Tokenizer.tokenize(base.doc(id));
                if (!td.contains(modello) || !td.contains(parola)) varianti = true;
            }
            esatte.add(new Query(modello + " " + parola, rilevanti, varianti));
            jolly.add(new Query(modello.substring(0, (int) Math.ceil(modello.length() * 0.6)) + "*", rilevanti, varianti));
        }

        PrintStream out = new PrintStream(Files.newOutputStream(Path.of("data/risultati_benchmark.txt")), true);
        for (PrintStream o : new PrintStream[]{System.out, out}) {
            o.printf("corpus: %d righe, %d termini; query esatte: %d, con wildcard: %d (di cui con almeno una riga letta diversamente: %d)%n",
                    base.size(), base.terms().size(), esatte.size(), jolly.size(), esatte.stream().filter(Query::conVarianti).count());
        }
        for (PrintStream o : new PrintStream[]{System.out, out}) {
            o.println("\nsistema                         set          P      R      F1     acc");
        }
        Object[][] sistemi = {
                {"esatta", base, Searcher.ModoFuzzy.NO},
                {"fuzzy (solo se assente)", base, Searcher.ModoFuzzy.FALLBACK},
                {"fuzzy sempre", base, Searcher.ModoFuzzy.SEMPRE},
                {"correzione OCR", corretto, Searcher.ModoFuzzy.NO},
                {"correzione OCR + fuzzy sempre", corretto, Searcher.ModoFuzzy.SEMPRE},
        };
        for (Object[] s : sistemi) {
            Searcher se = new Searcher((InvertedIndex) s[1], (Searcher.ModoFuzzy) s[2]);
            for (PrintStream o : new PrintStream[]{System.out, out}) {
                riga(o, (String) s[0], "tutte", valuta(se::search, esatte, false, base.size()));
                riga(o, "", "con varianti", valuta(se::search, esatte, true, base.size()));
            }
        }
        // confronto con libreria di terzi (fastText): addestrato sul testo del corpus, non sull'indice corretto
        FastTextConfronto ft = new FastTextConfronto(base);
        for (PrintStream o : new PrintStream[]{System.out, out}) {
            riga(o, "fastText (LIBRERIA, confronto)", "tutte", valuta(ft::search, esatte, false, base.size()));
            riga(o, "", "con varianti", valuta(ft::search, esatte, true, base.size()));
        }
        for (PrintStream o : new PrintStream[]{System.out, out}) o.println("\nwildcard (prefisso del modello + '*'), indice senza/con correzione");
        for (Object[] s : new Object[][]{{"wildcard", base}, {"wildcard su indice corretto", corretto}}) {
            Searcher se = new Searcher((InvertedIndex) s[1], Searcher.ModoFuzzy.NO);
            for (PrintStream o : new PrintStream[]{System.out, out}) {
                riga(o, (String) s[0], "tutte", valuta(se::search, jolly, false, base.size()));
                riga(o, "", "con varianti", valuta(se::search, jolly, true, base.size()));
            }
        }

        // ranking: stessa collezione, ma qui conta l'ORDINE dei risultati (MAP, P@1, R-precision)
        Object[][] ordinamenti = {
                {"AND, nessun punteggio (docId)", Searcher.ModoFuzzy.NO, Ranker.Modello.NESSUNO, true},
                {"AND + TF-IDF", Searcher.ModoFuzzy.NO, Ranker.Modello.TFIDF, true},
                {"AND + BM25", Searcher.ModoFuzzy.NO, Ranker.Modello.BM25, true},
                {"OR, nessun punteggio (docId)", Searcher.ModoFuzzy.NO, Ranker.Modello.NESSUNO, false},
                {"OR + TF-IDF", Searcher.ModoFuzzy.NO, Ranker.Modello.TFIDF, false},
                {"OR + BM25", Searcher.ModoFuzzy.NO, Ranker.Modello.BM25, false},
                {"fuzzy sempre, AND + TF-IDF", Searcher.ModoFuzzy.SEMPRE, Ranker.Modello.TFIDF, true},
                {"fuzzy sempre, AND + BM25", Searcher.ModoFuzzy.SEMPRE, Ranker.Modello.BM25, true},
        };
        for (PrintStream o : new PrintStream[]{System.out, out}) {
            o.println("\nranking (query esatte; ordine dei risultati)       set          MAP    P@1    R-prec");
        }
        for (Object[] r : ordinamenti) {
            Searcher se = new Searcher(base, (Searcher.ModoFuzzy) r[1]);
            Ranker.Modello m = (Ranker.Modello) r[2];
            boolean and = (Boolean) r[3];
            for (PrintStream o : new PrintStream[]{System.out, out}) {
                rigaRanking(o, (String) r[0], "tutte", valutaRanking(q -> se.searchRanked(q, m, and), esatte, false));
                rigaRanking(o, "", "con varianti", valutaRanking(q -> se.searchRanked(q, m, and), esatte, true));
            }
        }

        // efficienza delle skip list: confronti fra docId, AND di (modello, parola) sulle query esatte
        long[] cSkip = {0}, cLin = {0};
        for (Query q : esatte) {
            List<String> ts = Tokenizer.tokenize(q.testo());
            PostingList a = base.postings(ts.get(0)), b = base.postings(ts.get(1));
            PostingList.intersect(a, b, cSkip);
            PostingList.intersectLineare(a, b, cLin);
        }
        // AND di un termine raro con uno molto frequente (caso favorevole agli skip)
        String frequente = base.terms().stream().max((x, y) -> base.postings(x).size() - base.postings(y).size()).get();
        long[] sk2 = {0}, li2 = {0};
        for (Query q : esatte) {
            PostingList a = base.postings(Tokenizer.tokenize(q.testo()).get(0)), b = base.postings(frequente);
            PostingList.intersect(a, b, sk2);
            PostingList.intersectLineare(a, b, li2);
        }
        for (PrintStream o : new PrintStream[]{System.out, out}) {
            o.printf("%nconfronti fra docId (intersezione di 2 termini, %d query)%n", esatte.size());
            o.printf("  modello AND parola:         lineare %d, con skip %d%n", cLin[0], cSkip[0]);
            o.printf("  modello AND '%s' (df %d): lineare %d, con skip %d%n", frequente, base.postings(frequente).size(), li2[0], sk2[0]);
        }
    }

    static double[] valuta(Function<String, List<Integer>> ricerca, List<Query> qs, boolean soloVarianti, int nDocs) {
        double p = 0, r = 0, f = 0, acc = 0;
        int n = 0;
        for (Query q : qs) {
            if (soloVarianti && !q.conVarianti()) continue;
            Set<Integer> trovati = new HashSet<>(ricerca.apply(q.testo()));
            int tp = 0;
            for (int d : trovati) if (q.rilevanti().contains(d)) tp++;
            double pq = trovati.isEmpty() ? 0 : (double) tp / trovati.size();
            double rq = (double) tp / q.rilevanti().size();
            p += pq;
            r += rq;
            f += pq + rq == 0 ? 0 : 2 * pq * rq / (pq + rq);
            int fp = trovati.size() - tp, fn = q.rilevanti().size() - tp;
            acc += (double) (nDocs - fp - fn) / nDocs;
            n++;
        }
        return new double[]{p / n, r / n, f / n, acc / n, n};
    }

    static void riga(PrintStream o, String nome, String set, double[] m) {
        o.printf("%-31s %-12s %.3f  %.3f  %.3f  %.4f  (n=%d)%n", nome, set, m[0], m[1], m[2], m[3], (int) m[4]);
    }

    /** MAP, P@1 e R-precision (precisione alla posizione |Rel|) sulla lista ordinata dei risultati. */
    static double[] valutaRanking(Function<String, List<Integer>> ricerca, List<Query> qs, boolean soloVarianti) {
        double ap = 0, p1 = 0, rp = 0;
        int n = 0;
        for (Query q : qs) {
            if (soloVarianti && !q.conVarianti()) continue;
            List<Integer> lista = ricerca.apply(q.testo());
            int trovati = 0, trovatiInTopR = 0;
            double somma = 0;
            for (int k = 0; k < lista.size(); k++) {
                if (q.rilevanti().contains(lista.get(k))) {
                    trovati++;
                    somma += (double) trovati / (k + 1);
                    if (k < q.rilevanti().size()) trovatiInTopR++;
                }
            }
            rp += (double) trovatiInTopR / q.rilevanti().size(); // R-precision: rilevanti fra i primi |Rel|, diviso |Rel|
            ap += somma / q.rilevanti().size();
            if (!lista.isEmpty() && q.rilevanti().contains(lista.get(0))) p1 += 1;
            n++;
        }
        return new double[]{ap / n, p1 / n, rp / n, n};
    }

    static void rigaRanking(PrintStream o, String nome, String set, double[] m) {
        o.printf("%-45s %-12s %.3f  %.3f  %.3f  (n=%d)%n", nome, set, m[0], m[1], m[2], (int) m[3]);
    }
}
