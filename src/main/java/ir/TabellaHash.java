package ir;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Tabella hash String -> V con liste di collisione (nessuna collezione di java.util nella struttura).
 * Funzione di hash FNV-1a a 32 bit; numero di bucket potenza di 2 (indice = hash & (n-1));
 * raddoppio e redistribuzione (rehash) quando il fattore di carico supera 0,75.
 * Ogni bucket e' una lista concatenata di nodi (inserimento in testa).
 */
public final class TabellaHash<V> {
    private static final double CARICO_MAX = 0.75;

    private static final class Nodo<V> {
        final String chiave;
        final int hash;
        V valore;
        Nodo<V> prossimo;
        Nodo(String chiave, int hash, V valore, Nodo<V> prossimo) {
            this.chiave = chiave; this.hash = hash; this.valore = valore; this.prossimo = prossimo;
        }
    }

    private Nodo<V>[] bucket;
    private int n;

    public TabellaHash() { this(16); }

    @SuppressWarnings("unchecked")
    public TabellaHash(int bucketIniziali) {
        int b = 16;
        while (b < bucketIniziali) b <<= 1;
        bucket = (Nodo<V>[]) new Nodo[b];
    }

    static int hash(String s) {
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h ^ (h >>> 16);
    }

    public int size() { return n; }

    public V get(String chiave) {
        int h = hash(chiave);
        for (Nodo<V> x = bucket[h & (bucket.length - 1)]; x != null; x = x.prossimo)
            if (x.hash == h && x.chiave.equals(chiave)) return x.valore;
        return null;
    }

    public boolean contains(String chiave) { return get(chiave) != null; }

    public V getOrDefault(String chiave, V predefinito) {
        V v = get(chiave);
        return v != null ? v : predefinito;
    }

    /** Inserisce o sostituisce; il valore null non e' ammesso. */
    public void put(String chiave, V valore) {
        if (valore == null) throw new IllegalArgumentException("valore null");
        int h = hash(chiave);
        int i = h & (bucket.length - 1);
        for (Nodo<V> x = bucket[i]; x != null; x = x.prossimo)
            if (x.hash == h && x.chiave.equals(chiave)) { x.valore = valore; return; }
        bucket[i] = new Nodo<>(chiave, h, valore, bucket[i]);
        if (++n > CARICO_MAX * bucket.length) raddoppia();
    }

    public V computeIfAbsent(String chiave, Function<String, V> crea) {
        V v = get(chiave);
        if (v == null) { v = crea.apply(chiave); put(chiave, v); }
        return v;
    }

    @SuppressWarnings("unchecked")
    private void raddoppia() {
        Nodo<V>[] vecchio = bucket;
        bucket = (Nodo<V>[]) new Nodo[vecchio.length * 2];
        for (Nodo<V> testa : vecchio) {
            for (Nodo<V> x = testa; x != null; ) {
                Nodo<V> prossimo = x.prossimo;
                int i = x.hash & (bucket.length - 1);
                x.prossimo = bucket[i];
                bucket[i] = x;
                x = prossimo;
            }
        }
    }

    /** Chiavi in ordine non specificato. */
    public List<String> chiavi() {
        List<String> out = new ArrayList<>(n);
        for (Nodo<V> testa : bucket) for (Nodo<V> x = testa; x != null; x = x.prossimo) out.add(x.chiave);
        return out;
    }

    // ---- statistiche (usate dal confronto fra strutture) ----

    public int numeroBucket() { return bucket.length; }

    public double fattoreDiCarico() { return (double) n / bucket.length; }

    /** Lunghezza della catena piu' lunga. */
    public int catenaMassima() {
        int max = 0;
        for (Nodo<V> testa : bucket) {
            int l = 0;
            for (Nodo<V> x = testa; x != null; x = x.prossimo) l++;
            max = Math.max(max, l);
        }
        return max;
    }

    /** Numero di bucket non vuoti. */
    public int bucketOccupati() {
        int c = 0;
        for (Nodo<V> testa : bucket) if (testa != null) c++;
        return c;
    }

    /** Nodi visitati da una ricerca con successo della chiave (0 se assente). */
    public int passiRicerca(String chiave) {
        int h = hash(chiave), p = 0;
        for (Nodo<V> x = bucket[h & (bucket.length - 1)]; x != null; x = x.prossimo) {
            p++;
            if (x.hash == h && x.chiave.equals(chiave)) return p;
        }
        return 0;
    }
}
