package ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class WebServerTest {
    private WebServer web() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("100 ARIE friggitrice <b>aria</b>");
        ix.add("101 SEKO frigo 2P");
        return new WebServer(ix);
    }

    @Test
    void risultatiEEscape() {
        String h = web().pagina("fri*", Searcher.ModoFuzzy.NO, false);
        assertTrue(h.contains("2 risultati"));
        assertTrue(h.contains("&lt;b&gt;aria&lt;/b&gt;"));
        assertFalse(h.contains("<b>aria</b>"));
    }

    @Test
    void queryVuotaENonRiflessa() {
        assertFalse(web().pagina("", Searcher.ModoFuzzy.NO, false).contains("risultati"));
        assertTrue(web().pagina("\"><script>", Searcher.ModoFuzzy.NO, false).contains("&quot;&gt;&lt;script&gt;"));
    }

    @Test
    void ordinamentoEOr() {
        WebServer w = web();
        String h = w.pagina("friggitrice aria", Searcher.ModoFuzzy.NO, false, Ranker.Modello.BM25, true);
        assertTrue(h.contains("1 risultati"));
        assertTrue(h.contains("<option value=\"BM25\" selected>"));
        assertTrue(h.contains("name=\"or\" value=\"1\" checked"));
        assertTrue(w.pagina("friggitrice frigo", Searcher.ModoFuzzy.NO, false, Ranker.Modello.TFIDF, true).contains("2 risultati"));
        assertTrue(w.pagina("friggitrice frigo", Searcher.ModoFuzzy.NO, false, Ranker.Modello.TFIDF, false).contains("0 risultati"));
    }

    @Test
    void parametriUrl() {
        assertEquals(Map.of("q", "lava*", "ocr", "1"), WebServer.parametri("q=lava%2A&ocr=1"));
    }
}
