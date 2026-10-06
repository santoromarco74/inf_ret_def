package ir;

import java.util.HashMap;
import java.util.Map;

/**
 * Punteggio dei documenti: TF-IDF e BM25 calcolati sulle frequenze dei termini dell'indice.
 *
 * TF-IDF (schema SMART lnc.ltn, Manning-Raghavan-Schutze cap. 6): peso del termine nel documento
 * (1 + ln tf), normalizzato col coseno sul vettore del documento; peso del termine nella query = idf = ln(N/df).
 *   score(d,q) = sum_t  (1 + ln tf_td) / ||d||  *  ln(N / df_t)
 * BM25 (Robertson), idf non negativo:
 *   score(d,q) = sum_t  ln(1 + (N - df_t + 0.5)/(df_t + 0.5))  *  tf (k1+1) / (tf + k1 (1 - b + b |d|/avgdl))
 * con k1 = 1,2 e b = 0,75 (valori standard, fissati a priori e non ottimizzati sul benchmark).
 *
 * Una parola della query puo' espandersi in piu' termini (wildcard, fuzzy): il punteggio e' la somma sui termini espansi.
 */
public final class Ranker {
    public enum Modello { NESSUNO, TFIDF, BM25 }

    static final double K1 = 1.2, B = 0.75;

    private final Indice ix;
    private double[] norme; // ||d|| per il TF-IDF, calcolate alla prima richiesta

    public Ranker(Indice ix) { this.ix = ix; }

    /** Punteggio di ogni documento che contiene almeno uno dei termini dati. */
    public Map<Integer, Double> punteggi(Iterable<String> termini, Modello m) {
        Map<Integer, Double> out = new HashMap<>();
        if (m == Modello.NESSUNO) {
            for (String t : termini) {
                PostingList p = ix.postings(t);
                for (int i = 0; i < p.size(); i++) out.merge(p.get(i), 0.0, Double::sum);
            }
            return out;
        }
        double n = ix.size(), avg = ix.lunghezzaMedia();
        for (String t : termini) {
            PostingList p = ix.postings(t);
            if (p.size() == 0) continue;
            int[] tf = ix.tf(t);
            double df = p.size();
            for (int i = 0; i < p.size(); i++) {
                int d = p.get(i);
                double s;
                if (m == Modello.TFIDF) {
                    s = (1 + Math.log(tf[i])) / norma(d) * Math.log(n / df);
                } else {
                    double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                    s = idf * tf[i] * (K1 + 1) / (tf[i] + K1 * (1 - B + B * ix.lunghezza(d) / avg));
                }
                out.merge(d, s, Double::sum);
            }
        }
        return out;
    }

    private double norma(int d) {
        if (norme == null) {
            double[] q = new double[ix.size()];
            for (String t : ix.terms()) {
                PostingList p = ix.postings(t);
                int[] tf = ix.tf(t);
                for (int i = 0; i < p.size(); i++) {
                    double w = 1 + Math.log(tf[i]);
                    q[p.get(i)] += w * w;
                }
            }
            for (int i = 0; i < q.length; i++) q[i] = Math.sqrt(q[i]);
            norme = q;
        }
        return norme[d];
    }
}
