package ir;

import java.util.Arrays;

/**
 * Heap binario su array (nessuna collezione di java.util) per i primi k risultati del ranking.
 * Min-heap sul "peggiore": la radice e' il documento peggiore fra quelli tenuti (punteggio piu' basso;
 * a parita' docId piu' alto). Con k risultati richiesti su n candidati costa O(n log k) invece dell'ordinamento
 * completo O(n log n): un candidato entra solo se e' migliore della radice, che sostituisce.
 */
public final class HeapBinario {
    private int[] ids;
    private double[] punti;
    private int n;

    public HeapBinario(int capacitaIniziale) {
        ids = new int[Math.max(4, capacitaIniziale)];
        punti = new double[ids.length];
    }

    public int size() { return n; }

    /** true se (idA, a) e' peggiore di (idB, b). */
    private static boolean peggiore(double a, int idA, double b, int idB) {
        return a < b || (a == b && idA > idB);
    }

    public void aggiungi(int id, double punteggio) {
        if (n == ids.length) { ids = Arrays.copyOf(ids, n * 2); punti = Arrays.copyOf(punti, n * 2); }
        int i = n++;
        while (i > 0) {                                 // risale finche' e' peggiore del padre
            int p = (i - 1) / 2;
            if (!peggiore(punteggio, id, punti[p], ids[p])) break;
            ids[i] = ids[p]; punti[i] = punti[p];
            i = p;
        }
        ids[i] = id; punti[i] = punteggio;
    }

    /** Punteggio del documento peggiore tenuto (radice). */
    public double punteggioPeggiore() { return punti[0]; }

    public int idPeggiore() { return ids[0]; }

    /** Estrae il documento peggiore. */
    public int estraiPeggiore() {
        int radice = ids[0];
        n--;
        if (n > 0) scendi(ids[n], punti[n]);
        return radice;
    }

    private void scendi(int id, double punteggio) {
        int i = 0;
        while (true) {
            int f = 2 * i + 1;
            if (f >= n) break;
            if (f + 1 < n && peggiore(punti[f + 1], ids[f + 1], punti[f], ids[f])) f++;
            if (!peggiore(punti[f], ids[f], punteggio, id)) break;
            ids[i] = ids[f]; punti[i] = punti[f];
            i = f;
        }
        ids[i] = id; punti[i] = punteggio;
    }

    /**
     * I migliori k documenti, dal migliore al peggiore (punteggio decrescente, a parita' docId crescente).
     * punteggi[i] e' il punteggio del documento ids[i].
     */
    public static int[] topK(int[] ids, double[] punteggi, int k) {
        if (k <= 0 || ids.length == 0) return new int[0];
        HeapBinario h = new HeapBinario(Math.min(k, ids.length));
        for (int i = 0; i < ids.length; i++) {
            if (h.size() < k) h.aggiungi(ids[i], punteggi[i]);
            else if (peggiore(h.punteggioPeggiore(), h.idPeggiore(), punteggi[i], ids[i])) {
                h.estraiPeggiore();
                h.aggiungi(ids[i], punteggi[i]);
            }
        }
        int[] out = new int[h.size()];
        for (int i = out.length - 1; i >= 0; i--) out[i] = h.estraiPeggiore();
        return out;
    }
}
