package ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** TabellaHash, AlberoBinarioRicerca e HeapBinario verificati contro java.util (usata solo come riferimento nel test). */
class DizionarioStruttureTest {
    private static String casuale(Random r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1 + r.nextInt(6); i > 0; i--) sb.append((char) ('a' + r.nextInt(5)));
        return sb.toString();
    }

    @Test
    void tabellaHashEquivaleAUnaMappa() {
        Random r = new Random(7);
        TabellaHash<Integer> t = new TabellaHash<>();
        Map<String, Integer> rif = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            String k = casuale(r);
            t.put(k, i);
            rif.put(k, i);
        }
        assertEquals(rif.size(), t.size());
        for (Map.Entry<String, Integer> e : rif.entrySet()) assertEquals(e.getValue(), t.get(e.getKey()));
        assertNull(t.get("zzzzzz"));
        assertFalse(t.contains("zzzzzz"));
        assertEquals(rif.size(), t.chiavi().size());
        assertTrue(t.fattoreDiCarico() <= 0.75, "carico " + t.fattoreDiCarico());
    }

    @Test
    void tabellaHashRehashConservaLeChiavi() {
        TabellaHash<String> t = new TabellaHash<>(16);
        int bucketPrima = t.numeroBucket();
        for (int i = 0; i < 1000; i++) t.put("k" + i, "v" + i);
        assertTrue(t.numeroBucket() > bucketPrima);
        for (int i = 0; i < 1000; i++) assertEquals("v" + i, t.get("k" + i));
        t.put("k5", "nuovo");
        assertEquals("nuovo", t.get("k5"));
        assertEquals(1000, t.size());
    }

    @Test
    void bstConInserimentoOrdinatoDiventaUnaListaMaRestaCorretto() {
        AlberoBinarioRicerca a = new AlberoBinarioRicerca();
        List<String> chiavi = new ArrayList<>();
        for (int i = 0; i < 2000; i++) chiavi.add(String.format("t%05d", i));
        for (String k : chiavi) a.inserisci(k); // caso peggiore: nessun bilanciamento
        assertEquals(2000, a.size());
        assertEquals(2000, a.altezza());
        assertEquals(chiavi, a.inOrdine());
        assertEquals(List.of("t00010", "t00011"), a.conPrefisso("t0001").subList(0, 2));
    }

    @Test
    void bstDaOrdinatiHaAltezzaMinima() {
        for (int n : new int[]{0, 1, 2, 3, 7, 8, 1000, 3046}) {
            List<String> chiavi = new ArrayList<>();
            for (int i = 0; i < n; i++) chiavi.add(String.format("t%05d", i));
            AlberoBinarioRicerca a = AlberoBinarioRicerca.daOrdinati(chiavi);
            assertEquals(n, a.size());
            assertEquals(32 - Integer.numberOfLeadingZeros(n), a.altezza(), "n=" + n); // ceil(log2(n+1))
            assertEquals(chiavi, a.inOrdine());
        }
    }

    @Test
    void bstPrefissoEquivaleAFiltroSulRiferimento() {
        Random r = new Random(3);
        AlberoBinarioRicerca a = new AlberoBinarioRicerca();
        TreeSet<String> rif = new TreeSet<>();
        for (int i = 0; i < 5000; i++) {
            String k = casuale(r);
            a.inserisci(k);
            a.inserisci(k); // duplicati ignorati
            rif.add(k);
        }
        assertEquals(rif.size(), a.size());
        assertEquals(new ArrayList<>(rif), a.inOrdine());
        for (String p : new String[]{"a", "ab", "abc", "e", "ddd", "zz", "b"}) {
            List<String> atteso = new ArrayList<>();
            for (String k : rif) if (k.startsWith(p)) atteso.add(k);
            assertEquals(atteso, a.conPrefisso(p), "prefisso " + p);
        }
        assertTrue(a.contiene(rif.first()));
        assertFalse(a.contiene("zzzz"));
    }

    @Test
    void heapTopKEquivaleAOrdinamentoCompleto() {
        Random r = new Random(11);
        int n = 3000;
        int[] ids = new int[n];
        double[] pt = new double[n];
        for (int i = 0; i < n; i++) { ids[i] = i; pt[i] = r.nextInt(50); } // molti punteggi uguali: spareggio per docId
        List<Integer> rif = new ArrayList<>();
        for (int i = 0; i < n; i++) rif.add(i);
        rif.sort((x, y) -> pt[y] != pt[x] ? Double.compare(pt[y], pt[x]) : Integer.compare(x, y));
        for (int k : new int[]{1, 10, 100, n, n + 50}) {
            int[] top = HeapBinario.topK(ids, pt, k);
            assertEquals(Math.min(k, n), top.length);
            for (int i = 0; i < top.length; i++) assertEquals(rif.get(i).intValue(), top[i], "k=" + k + " posizione " + i);
        }
        assertEquals(0, HeapBinario.topK(ids, pt, 0).length);
    }

    @Test
    void indiceInvertitoConLeNuoveStruttureDaTerminiOrdinatiEPrefissi() {
        InvertedIndex ix = new InvertedIndex();
        ix.add("vite zincata 8x40");
        ix.add("vitone acciaio");
        ix.add("bullone vite");
        assertEquals(List.of("8x40", "acciaio", "bullone", "vite", "vitone", "zincata"), ix.terms());
        assertEquals(List.of("vite", "vitone"), ix.terminiConPrefisso("vit"));
        assertEquals(2, ix.postings("vite").size());
        ix.add("vite nuova"); // l'aggiunta invalida la PostingList gia' costruita
        assertEquals(3, ix.postings("vite").size());
    }
}
