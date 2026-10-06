package ir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class StruttureTest {
    private static PostingList casuale(Random r, int n, int max) {
        TreeSet<Integer> s = new TreeSet<>();
        while (s.size() < n) s.add(r.nextInt(max));
        return new PostingList(s.stream().mapToInt(Integer::intValue).toArray());
    }

    @Test
    void skipEquivaleALineareEConfrontaMeno() {
        Random r = new Random(1);
        long[] cs = {0}, cl = {0};
        for (int t = 0; t < 200; t++) {
            PostingList a = casuale(r, 20, 100000), b = casuale(r, 5000, 100000);
            assertArrayEquals(PostingList.intersectLineare(a, b, cl).toArray(), PostingList.intersect(a, b, cs).toArray());
        }
        assertTrue(cs[0] < cl[0], "skip " + cs[0] + " vs lineare " + cl[0]);
    }

    @Test
    void unione() {
        PostingList u = PostingList.union(new PostingList(new int[]{1, 3, 5}), new PostingList(new int[]{3, 4}));
        assertArrayEquals(new int[]{1, 3, 4, 5}, u.toArray());
    }

    @Test
    void levenshtein() {
        assertEquals(1, EditDistance.levenshtein("tasto", "tasta"));
        assertEquals(3, EditDistance.levenshtein("kitten", "sitting"));
    }

    private InvertedIndex indice() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("TV LED 55 UHD SMART");
        ix.add("LAVATRICE 7KG SLIM");
        ix.add("LAVASTOVIGLIE 13COP");
        ix.add("CUFFIA AURIC BT TASTO");
        return ix;
    }

    @Test
    void wildcard() {
        Searcher s = new Searcher(indice(), false);
        assertEquals(List.of(1, 2), s.search("lava*"));
        assertEquals(List.of(1), s.search("*trice"));
        assertEquals(List.of(2), s.search("l*glie"));
        assertEquals(List.of(1, 2), s.search("*av*"));
        assertEquals(List.of(), s.search("zz*"));
    }

    @Test
    void fuzzy() {
        assertEquals(List.of(3), new Searcher(indice(), true).search("tasta"));
        assertEquals(List.of(), new Searcher(indice(), false).search("tasta"));
        assertEquals(List.of(1), new Searcher(indice(), true).search("lavatrice 7kq"));
    }
}
