package ir;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dizionario dei termini non compresso, usato nell'indice persistente: i termini in ordine alfabetico, ognuno scritto
 * per intero (lunghezza in VByte + byte UTF-8). L'id di un termine e' la sua posizione nell'ordine alfabetico,
 * e si trova con una ricerca binaria.
 */
public final class DizionarioOrdinato {
    private final String[] termini;

    public DizionarioOrdinato(List<String> terminiOrdinati) {
        termini = terminiOrdinati.toArray(new String[0]);
    }

    private DizionarioOrdinato(String[] termini) {
        this.termini = termini;
    }

    public int size() { return termini.length; }

    /** Id del termine (posizione nell'ordine alfabetico) oppure -1. */
    public int id(String termine) {
        int lo = 0, hi = termini.length - 1;
        while (lo <= hi) {
            int mezzo = (lo + hi) >>> 1;
            int c = termini[mezzo].compareTo(termine);
            if (c == 0) return mezzo;
            if (c < 0) lo = mezzo + 1; else hi = mezzo - 1;
        }
        return -1;
    }

    public List<String> terms() { return new ArrayList<>(Arrays.asList(termini)); }

    private byte[] codifica() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String t : termini) {
            byte[] b = t.getBytes(StandardCharsets.UTF_8);
            VByte.scrivi(out, b.length);
            out.writeBytes(b);
        }
        return out.toByteArray();
    }

    /** Byte occupati nel file: per ogni termine la lunghezza (VByte) e i byte UTF-8. */
    public int byteOccupati() { return codifica().length; }

    public void scrivi(DataOutputStream out) throws IOException {
        byte[] dati = codifica();
        out.writeInt(termini.length);
        out.writeInt(dati.length);
        out.write(dati);
    }

    public static DizionarioOrdinato leggi(DataInputStream in) throws IOException {
        String[] t = new String[in.readInt()];
        byte[] dati = new byte[in.readInt()];
        in.readFully(dati);
        int[] pos = {0};
        for (int i = 0; i < t.length; i++) {
            int len = VByte.leggi(dati, pos);
            t[i] = new String(dati, pos[0], len, StandardCharsets.UTF_8);
            pos[0] += len;
        }
        return new DizionarioOrdinato(t);
    }
}
