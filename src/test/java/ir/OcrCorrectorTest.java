package ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OcrCorrectorTest {
    private InvertedIndex indice() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("PEN DRIVE 16GB CRUZER");   // 16gb: df 3
        ix.add("PEN DRIVE 16GB BLADE");
        ix.add("PEN DRIVE 16GB GLIDE");
        ix.add("PEN DRIVE 166B CRUZER");   // 166b: errore OCR (6 al posto di G, 8... )
        ix.add("PEN DRIVE 8GB CRUZER");    // 8gb vs 6gb: numeri diversi, mai toccati
        ix.add("PEN DRIVE 6GB CRUZER");
        return ix;
    }

    @Test
    void correggeSoloScambiConfusabiliVersoTermineFrequente() {
        Map<String, String> c = OcrCorrector.calcola(indice());
        assertEquals("16gb", c.get("166b"));
        assertFalse(c.containsKey("16gb"));
        assertFalse(c.containsKey("8gb"));
        assertFalse(c.containsKey("6gb"));
    }

    @Test
    void terminiCortiENumericiNonVengonoToccati() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("it 50 a");
        ix.add("it 50 a");
        ix.add("1t 5o 4");
        assertEquals(Map.of(), OcrCorrector.calcola(ix));
    }

    @Test
    void indiceCorrettoUnisceIPostings() {
        InvertedIndex ix = indice();
        InvertedIndex corretto = OcrCorrector.applica(ix, OcrCorrector.calcola(ix));
        assertEquals(List.of(0, 1, 2, 3), new Searcher(corretto, false).search("16gb"));
        assertEquals(List.of(), new Searcher(corretto, false).search("166b"));
    }
}
