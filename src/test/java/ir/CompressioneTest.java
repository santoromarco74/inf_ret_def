package ir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class CompressioneTest {
    @Test
    void vbyteRoundTrip() {
        int[] ids = {0, 1, 127, 128, 300, 16384, 16385, 2_000_000, Integer.MAX_VALUE / 2};
        assertArrayEquals(ids, VByte.decodificaPostings(VByte.codificaPostings(ids)));
        Random r = new Random(7);
        TreeSet<Integer> s = new TreeSet<>();
        while (s.size() < 2000) s.add(r.nextInt(1_000_000));
        int[] a = s.stream().mapToInt(Integer::intValue).toArray();
        assertArrayEquals(a, VByte.decodificaPostings(VByte.codificaPostings(a)));
    }

    @Test
    void dizionarioOrdinatoRoundTripERicerca() throws IOException {
        TreeSet<String> t = new TreeSet<>(List.of("lava", "lavasc", "lavastoviglie", "lavatrice", "lavatrici", "tv", "tasto",
                "tastiera", "vite", "zincata", "zucca", "è", "perché", "a", "ab", "abc", "abcd", "abcde"));
        List<String> termini = List.copyOf(t);
        DizionarioOrdinato d = new DizionarioOrdinato(termini);
        assertEquals(termini, d.terms());
        for (int i = 0; i < termini.size(); i++) assertEquals(i, d.id(termini.get(i)));
        assertEquals(-1, d.id("lavax"));
        assertEquals(-1, d.id("zzz"));
        assertEquals(-1, d.id("0"));
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        d.scrivi(new DataOutputStream(file));
        DizionarioOrdinato r = DizionarioOrdinato.leggi(new DataInputStream(new ByteArrayInputStream(file.toByteArray())));
        assertEquals(termini, r.terms());
        for (int i = 0; i < termini.size(); i++) assertEquals(i, r.id(termini.get(i)));
        assertEquals(-1, r.id("lavax"));
    }

    @Test
    void dizionarioOrdinatoVuoto() throws IOException {
        DizionarioOrdinato d = new DizionarioOrdinato(List.of());
        assertEquals(-1, d.id("a"));
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        d.scrivi(new DataOutputStream(file));
        assertEquals(0, DizionarioOrdinato.leggi(new DataInputStream(new ByteArrayInputStream(file.toByteArray()))).size());
    }

    @Test
    void indiceCompressoEquivaleAOriginale() {
        InvertedIndex ix = new InvertedIndex();
        Random r = new Random(3);
        String[] parole = {"tv", "led", "lavatrice", "slim", "cuffia", "bt", "nero", "bianco", "7kg", "8gb", "smart", "uhd", "4k"};
        for (int d = 0; d < 500; d++) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 6; k++) sb.append(parole[r.nextInt(parole.length)]).append(' ');
            ix.add(sb.toString());
        }
        CompressedIndex ci = new CompressedIndex(ix);
        assertEquals(ix.terms(), ci.terms());
        for (String t : ix.terms()) assertArrayEquals(ix.postings(t).toArray(), ci.postings(t).toArray());
        assertFalse(ci.contains("assente"));
        assertTrue(ci.postings("assente").size() == 0);
    }
}
