package ir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvertedIndexTest {
    private InvertedIndex build() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("Vite zincata 8x40 mm");
        ix.add("Rondella zincata 8 mm");
        ix.add("Vite inox 8x40");
        return ix;
    }

    @Test
    void tokenizza() {
        assertEquals(List.of("vite", "zincata", "8x40", "mm"), Tokenizer.tokenize("Vite zincata, 8x40 mm"));
    }

    @Test
    void postingsOrdinati() {
        assertArrayEquals(new int[]{0, 1}, build().postings("zincata").toArray());
    }

    @Test
    void andIntersezione() {
        assertEquals(List.of(0), build().searchAnd("vite zincata"));
        assertEquals(List.of(), build().searchAnd("vite rondella"));
    }
}
