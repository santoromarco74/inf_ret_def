package ir;

import java.io.ByteArrayOutputStream;

/**
 * Codifica a lunghezza variabile (variable byte): 7 bit di dato per byte,
 * il bit alto vale 1 sull'ultimo byte del numero. Usata per i gap dei postings e per le lunghezze del dizionario.
 */
public final class VByte {
    private VByte() {}

    public static void scrivi(ByteArrayOutputStream out, int n) {
        while (n >= 128) { out.write(n & 127); n >>>= 7; }
        out.write(n | 128);
    }

    /** Legge un numero a partire da {@code pos[0]} e avanza {@code pos[0]}. */
    public static int leggi(byte[] b, int[] pos) {
        int n = 0, shift = 0;
        while (true) {
            int x = b[pos[0]++] & 0xFF;
            if (x >= 128) return n | ((x & 127) << shift);
            n |= x << shift;
            shift += 7;
        }
    }

    /** Postings -> gap (primo docId, poi differenze) -> VByte. */
    public static byte[] codificaPostings(int[] docIds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int prec = 0;
        for (int id : docIds) { scrivi(out, id - prec); prec = id; }
        return out.toByteArray();
    }

    public static int[] decodificaPostings(byte[] b) {
        int[] tmp = new int[b.length]; // al piu' un numero per byte
        int n = 0, prec = 0;
        int[] pos = {0};
        while (pos[0] < b.length) { prec += leggi(b, pos); tmp[n++] = prec; }
        return java.util.Arrays.copyOf(tmp, n);
    }

    /** Sequenza di interi non negativi (non gap), per frequenze e lunghezze. */
    public static byte[] codificaInteri(int[] valori) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int v : valori) scrivi(out, v);
        return out.toByteArray();
    }

    public static int[] decodificaInteri(byte[] b) {
        int[] tmp = new int[b.length];
        int n = 0;
        int[] pos = {0};
        while (pos[0] < b.length) tmp[n++] = leggi(b, pos);
        return java.util.Arrays.copyOf(tmp, n);
    }
}
