package ir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BenchmarkTest {
    @Test
    void metriche() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("vite zincata");  // 0
        ix.add("vite inox");     // 1
        ix.add("rondella inox"); // 2
        ix.add("dado");          // 3
        Searcher s = new Searcher(ix, false);
        // query "vite": trova {0,1}; rilevanti {0,2} -> tp=1, P=1/2, R=1/2, F1=1/2, fp=1, fn=1, acc=(4-2)/4
        double[] m = Benchmark.valuta(s::search, List.of(new Benchmark.Query("vite", Set.of(0, 2), false)), false, 4);
        assertEquals(0.5, m[0], 1e-9);
        assertEquals(0.5, m[1], 1e-9);
        assertEquals(0.5, m[2], 1e-9);
        assertEquals(0.5, m[3], 1e-9);
    }
}
