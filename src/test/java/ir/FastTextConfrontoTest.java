package ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FastTextConfrontoTest {
    private FastTextConfronto modello() {
        InvertedIndex ix = new InvertedIndex();
        for (int i = 0; i < 20; i++) {
            ix.add("friggitrice aria 1800w nero");
            ix.add("lavatrice slim 7kg bianco");
        }
        try {
            return new FastTextConfronto(ix);
        } catch (Throwable e) { // libreria nativa non disponibile su questa piattaforma
            assumeTrue(false, "fastText non disponibile: " + e);
            return null;
        }
    }

    @Test
    void terminePresenteSiEspandeInSeStesso() throws Exception {
        FastTextConfronto ft = modello();
        List<String> e = ft.espandi("friggitrice");
        assertTrue(e.contains("friggitrice"));
        assertEquals(1, ft.search("lavatrice 7kg").stream().filter(d -> d == 1).count());
    }
}
