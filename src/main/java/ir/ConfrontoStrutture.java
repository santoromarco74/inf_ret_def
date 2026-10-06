package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Misure sulle strutture del dizionario: statistiche di TabellaHash e AlberoBinarioRicerca sui termini del corpus,
 * tempi di ricerca contro HashMap/TreeMap di java.util (solo termine di confronto), prefisso su albero contro
 * scansione del dizionario, top-k con HeapBinario contro ordinamento completo.
 * Conteggi e altezze sono deterministici; i tempi dipendono dalla macchina.
 * Uso: java -cp target/classes ir.ConfrontoStrutture
 */
public final class ConfrontoStrutture {
    private static final int GIRI = 5;

    public static void main(String[] args) throws IOException {
        InvertedIndex ix = new InvertedIndex();
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            ix.add(c[4] + " " + c[5]);
        }
        List<String> termini = ix.terms();
        int n = termini.size();
        System.out.printf("documenti %d, termini %d%n%n", ix.size(), n);

        TabellaHash<?> t = ix.tabella();
        long passi = 0;
        for (String s : termini) passi += t.passiRicerca(s);
        System.out.println("TabellaHash (dizionario dell'indice invertito)");
        System.out.printf("  bucket %d, occupati %d, fattore di carico %.2f%n", t.numeroBucket(), t.bucketOccupati(), t.fattoreDiCarico());
        System.out.printf("  catena piu' lunga %d, nodi visitati per ricerca (media) %.2f%n%n", t.catenaMassima(), (double) passi / n);

        AlberoBinarioRicerca a = ix.albero(); // termini inseriti nell'ordine in cui compaiono nel corpus
        long conf = 0;
        for (String s : termini) conf += a.confrontiRicerca(s);
        int minimo = 32 - Integer.numberOfLeadingZeros(n); // ceil(log2(n+1))
        System.out.println("AlberoBinarioRicerca (termini in ordine, non bilanciato)");
        System.out.printf("  inseriti nell'ordine del corpus: altezza %d (minimo possibile %d), confronti per ricerca (media) %.2f%n",
                a.altezza(), minimo, (double) conf / n);

        AlberoBinarioRicerca inOrdine = new AlberoBinarioRicerca(); // stessi termini inseriti in ordine alfabetico: l'albero diventa una lista
        for (String s : termini) inOrdine.inserisci(s);
        System.out.printf("  inseriti in ordine alfabetico: altezza %d%n", inOrdine.altezza());

        AlberoBinarioRicerca daOrdinati = AlberoBinarioRicerca.daOrdinati(termini); // termine di mezzo per primo, poi le due meta'
        long confOrdinati = 0;
        for (String s : termini) confOrdinati += daOrdinati.confrontiRicerca(s);
        System.out.printf("  costruito dai termini ordinati (termine di mezzo per primo): altezza %d, confronti per ricerca (media) %.2f%n%n",
                daOrdinati.altezza(), (double) confOrdinati / n);

        // ricerca di un termine: 200 giri su tutti i termini
        Map<String, Integer> hm = new HashMap<>();
        TreeMap<String, Integer> tm = new TreeMap<>();
        for (int i = 0; i < n; i++) { hm.put(termini.get(i), i); tm.put(termini.get(i), i); }
        int giri = 200;
        System.out.printf("Ricerca di un termine (%d ricerche, mediana di %d prove, ms)%n", n * giri, GIRI);
        System.out.printf("  TabellaHash %.1f | AlberoBinarioRicerca %.1f | HashMap (libreria) %.1f | TreeMap (libreria) %.1f%n%n",
                ms(() -> { int c = 0; for (int g = 0; g < giri; g++) for (String s : termini) if (t.contains(s)) c++; return c; }),
                ms(() -> { int c = 0; for (int g = 0; g < giri; g++) for (String s : termini) if (a.contiene(s)) c++; return c; }),
                ms(() -> { int c = 0; for (int g = 0; g < giri; g++) for (String s : termini) if (hm.containsKey(s)) c++; return c; }),
                ms(() -> { int c = 0; for (int g = 0; g < giri; g++) for (String s : termini) if (tm.containsKey(s)) c++; return c; }));

        // prefisso: tutti i prefissi di 3 lettere dei termini
        TreeSet<String> prefissi = new TreeSet<>();
        for (String s : termini) if (s.length() >= 3) prefissi.add(s.substring(0, 3));
        List<String> ps = new ArrayList<>(prefissi);
        int[] trovatiAlbero = {0}, trovatiScansione = {0};
        double msAlbero = ms(() -> { int c = 0; for (String p : ps) c += a.conPrefisso(p).size(); trovatiAlbero[0] = c; return c; });
        double msScan = ms(() -> {
            int c = 0;
            for (String p : ps) for (String s : termini) if (s.startsWith(p)) c++;
            trovatiScansione[0] = c;
            return c;
        });
        System.out.printf("Termini con un dato prefisso (%d prefissi di 3 lettere, ms)%n", ps.size());
        System.out.printf("  AlberoBinarioRicerca %.2f | scansione del dizionario %.2f | termini trovati %d = %d%n%n",
                msAlbero, msScan, trovatiAlbero[0], trovatiScansione[0]);

        // top-k su punteggi sintetici (il corpus ha liste troppo corte per vedere la differenza)
        int nn = 200_000, k = 10;
        Random r = new Random(1);
        int[] ids = new int[nn];
        double[] pt = new double[nn];
        for (int i = 0; i < nn; i++) { ids[i] = i; pt[i] = r.nextDouble(); }
        System.out.printf("Primi %d risultati su %d punteggi casuali (ms)%n", k, nn);
        System.out.printf("  HeapBinario %.1f | ordinamento completo (libreria) %.1f%n",
                ms(() -> HeapBinario.topK(ids, pt, k).length),
                ms(() -> {
                    Integer[] o = new Integer[nn];
                    for (int i = 0; i < nn; i++) o[i] = i;
                    java.util.Arrays.sort(o, (x, y) -> pt[y] != pt[x] ? Double.compare(pt[y], pt[x]) : Integer.compare(x, y));
                    return o[k - 1];
                }));
    }

    private static double ms(java.util.function.IntSupplier f) {
        double[] t = new double[GIRI];
        int sink = 0;
        for (int i = 0; i < GIRI + 2; i++) {
            long t0 = System.nanoTime();
            sink += f.getAsInt();
            if (i >= 2) t[i - 2] = (System.nanoTime() - t0) / 1e6; // le prime due prove scaldano la JVM
        }
        java.util.Arrays.sort(t);
        if (sink == 42) System.out.print("");
        return t[GIRI / 2];
    }
}
