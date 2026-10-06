package ir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RankerTest {
    private InvertedIndex indice() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("a a b");   // 0, lunghezza 3
        ix.add("a c");     // 1, lunghezza 2
        ix.add("b c c c"); // 2, lunghezza 4
        return ix;
    }

    @Test
    void frequenzeELunghezze() {
        InvertedIndex ix = indice();
        assertArrayEquals(new int[]{2, 1}, ix.tf("a"));
        assertArrayEquals(new int[]{1, 3}, ix.tf("c"));
        assertEquals(3, ix.lunghezza(0));
        assertEquals(4, ix.lunghezza(2));
        assertEquals(3.0, ix.lunghezzaMedia(), 1e-12);
    }

    @Test
    void bm25ValoriCalcolatiAMano() {
        // termine b: df=2, N=3 -> idf = ln(1.6) = 0.4700; doc0 (len 3 = avg): 2.2/2.2 = 1.0 ; doc2 (len 4): 2.2/2.5 = 0.88
        Map<Integer, Double> p = new Ranker(indice()).punteggi(List.of("b"), Ranker.Modello.BM25);
        assertEquals(0.4700, p.get(0), 1e-3);
        assertEquals(0.4136, p.get(2), 1e-3);
    }

    @Test
    void tfidfValoriCalcolatiAMano() {
        // termine c: idf = ln(3/2) = 0.4055. doc1: (1+ln1)/sqrt(2) * idf = 0.2867 ; doc2: (1+ln3)/sqrt(1+(1+ln3)^2) * idf = 0.3660
        Map<Integer, Double> p = new Ranker(indice()).punteggi(List.of("c"), Ranker.Modello.TFIDF);
        assertEquals(0.2867, p.get(1), 1e-3);
        assertEquals(0.3660, p.get(2), 1e-3);
    }

    @Test
    void ordinamentoAndOr() {
        Searcher s = new Searcher(indice(), Searcher.ModoFuzzy.NO);
        assertEquals(List.of(1), s.searchRanked("a c", Ranker.Modello.BM25, true));
        List<Integer> or = s.searchRanked("c", Ranker.Modello.TFIDF, false);
        assertEquals(List.of(2, 1), or); // tf piu' alto prima
        assertTrue(s.searchRanked("a c", Ranker.Modello.BM25, false).containsAll(List.of(0, 1, 2)));
        assertEquals(List.of(0, 2), s.searchRanked("b", Ranker.Modello.BM25, false)); // documento piu' corto prima
        assertEquals(List.of(0, 2), s.searchRanked("b", Ranker.Modello.NESSUNO, false)); // senza punteggio: per docId
    }
}
