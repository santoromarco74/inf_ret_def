package ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearcherDettaglioTest {
    private InvertedIndex tre() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("a a b");   // 0, lunghezza 3
        ix.add("a c");     // 1, lunghezza 2
        ix.add("b c c c"); // 2, lunghezza 4
        return ix;
    }

    private static List<Integer> ids(Searcher.Risposta r) {
        List<Integer> out = new ArrayList<>();
        for (Searcher.Risultato x : r.risultati()) out.add(x.docId());
        return out;
    }

    @Test
    void punteggioEOrdineCoincidonoConIlRanker() {
        // stessi valori calcolati a mano in RankerTest: termine c, doc2 0.3660, doc1 0.2867
        Searcher s = new Searcher(tre(), Searcher.ModoFuzzy.NO);
        Searcher.Risposta r = s.searchDettaglio("c", Ranker.Modello.TFIDF, false, 10);
        assertEquals(2, r.totale());
        assertEquals(List.of(2, 1), ids(r));
        assertEquals(0.3660, r.risultati().get(0).punteggio(), 1e-3);
        assertEquals(0.2867, r.risultati().get(1).punteggio(), 1e-3);
        assertEquals(List.of("c"), r.risultati().get(0).termini());
    }

    @Test
    void stessiIdDiSearchRanked() {
        Searcher s = new Searcher(tre(), Searcher.ModoFuzzy.NO);
        for (Ranker.Modello m : Ranker.Modello.values()) {
            for (boolean and : new boolean[]{true, false}) {
                for (String q : new String[]{"a c", "b", "a", "z", "c b"}) {
                    assertEquals(s.searchRanked(q, m, and), ids(s.searchDettaglio(q, m, and, Integer.MAX_VALUE)), q + " " + m + " " + and);
                }
            }
        }
    }

    @Test
    void senzaModelloIlPunteggioEZero() {
        Searcher s = new Searcher(tre(), Searcher.ModoFuzzy.NO);
        for (Searcher.Risultato x : s.searchDettaglio("b", Ranker.Modello.NESSUNO, false, 10).risultati()) assertEquals(0.0, x.punteggio());
    }

    @Test
    void andElencaITerminiDiTutteLeParole() {
        Searcher s = new Searcher(tre(), Searcher.ModoFuzzy.NO);
        Searcher.Risposta r = s.searchDettaglio("a c", Ranker.Modello.BM25, true, 10);
        assertEquals(List.of(1), ids(r));
        assertEquals(List.of("a", "c"), r.risultati().get(0).termini());
    }

    @Test
    void terminiConWildcardEFuzzy() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("100 ARIE friggitrice aria"); // 0
        ix.add("101 SEKO frigo 2P");         // 1
        ix.add("102 lavatrice slim");        // 2
        Searcher exact = new Searcher(ix, Searcher.ModoFuzzy.NO);
        Searcher.Risposta w = exact.searchDettaglio("fri*", Ranker.Modello.NESSUNO, false, 10);
        assertEquals(List.of(0, 1), ids(w));
        assertEquals(List.of("friggitrice"), w.risultati().get(0).termini());
        assertEquals(List.of("frigo"), w.risultati().get(1).termini());

        Searcher fuzzy = new Searcher(ix, Searcher.ModoFuzzy.FALLBACK); // 'frigitrice' non e' nel dizionario
        Searcher.Risposta f = fuzzy.searchDettaglio("frigitrice", Ranker.Modello.NESSUNO, false, 10);
        assertEquals(List.of(0), ids(f));
        assertEquals(List.of("friggitrice"), f.risultati().get(0).termini());
    }

    @Test
    void soloIPrimiKMaTotaleCompleto() {
        InvertedIndex ix = new InvertedIndex();
        for (int i = 0; i < 5; i++) ix.add("x y" + i);
        Searcher.Risposta r = new Searcher(ix, Searcher.ModoFuzzy.NO).searchDettaglio("x", Ranker.Modello.NESSUNO, false, 2);
        assertEquals(5, r.totale());
        assertEquals(List.of(0, 1), ids(r));
    }

    @Test
    void queryVuota() {
        Searcher.Risposta r = new Searcher(tre(), Searcher.ModoFuzzy.NO).searchDettaglio("   ", Ranker.Modello.BM25, true, 10);
        assertEquals(0, r.totale());
        assertTrue(r.risultati().isEmpty());
    }

    @Test
    void postingListContiene() {
        PostingList p = new PostingList(new int[]{2, 5, 9, 14});
        for (int d : new int[]{2, 5, 9, 14}) assertTrue(p.contiene(d), "presente " + d);
        for (int d : new int[]{0, 3, 10, 15, -1}) assertFalse(p.contiene(d), "assente " + d);
        assertFalse(PostingList.VUOTA.contiene(0));
    }
}
