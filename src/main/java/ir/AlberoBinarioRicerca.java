package ir;

import java.util.ArrayList;
import java.util.List;

/**
 * Insieme ordinato di stringhe su albero binario di ricerca (nessuna collezione di java.util nella struttura):
 * a sinistra di ogni nodo stanno le chiavi minori, a destra le maggiori.
 * L'albero NON e' bilanciato: la sua altezza dipende dall'ordine di inserimento (con chiavi inserite in ordine
 * alfabetico diventa una lista). {@link #daOrdinati} costruisce da una lista gia' ordinata un albero di altezza minima.
 * Serve come dizionario ordinato dei termini: visita in-order (ordine alfabetico) e ricerca per prefisso
 * come visita limitata ai soli sottoalberi che possono contenere il prefisso.
 */
public final class AlberoBinarioRicerca {
    private static final class Nodo {
        final String chiave;
        Nodo sx, dx;
        Nodo(String chiave) { this.chiave = chiave; }
    }

    private Nodo radice;
    private int n;
    private int altezza;

    public int size() { return n; }

    /** Altezza dell'albero (numero di nodi del cammino piu' lungo; 0 se vuoto). */
    public int altezza() { return altezza; }

    /** Inserisce la chiave; le chiavi duplicate sono ignorate. */
    public void inserisci(String chiave) {
        if (radice == null) { radice = new Nodo(chiave); n = 1; altezza = 1; return; }
        Nodo x = radice;
        int livello = 1;
        while (true) {
            int c = chiave.compareTo(x.chiave);
            if (c == 0) return;
            livello++;
            if (c < 0) {
                if (x.sx == null) { x.sx = new Nodo(chiave); break; }
                x = x.sx;
            } else {
                if (x.dx == null) { x.dx = new Nodo(chiave); break; }
                x = x.dx;
            }
        }
        n++;
        if (livello > altezza) altezza = livello;
    }

    /**
     * Albero di altezza minima da chiavi gia' ordinate e senza duplicati: si inserisce prima la chiave di mezzo,
     * poi (allo stesso modo) quelle della meta' sinistra e della meta' destra.
     */
    public static AlberoBinarioRicerca daOrdinati(List<String> ordinate) {
        AlberoBinarioRicerca a = new AlberoBinarioRicerca();
        a.inserisciDiMezzo(ordinate, 0, ordinate.size() - 1);
        return a;
    }

    private void inserisciDiMezzo(List<String> chiavi, int da, int a) {
        if (da > a) return;
        int mezzo = (da + a) >>> 1;
        inserisci(chiavi.get(mezzo));
        inserisciDiMezzo(chiavi, da, mezzo - 1);
        inserisciDiMezzo(chiavi, mezzo + 1, a);
    }

    public boolean contiene(String chiave) { return confrontiRicerca(chiave) > 0; }

    /** Confronti eseguiti da una ricerca con successo (0 se la chiave e' assente). */
    public int confrontiRicerca(String chiave) {
        int conf = 0;
        for (Nodo x = radice; x != null; ) {
            conf++;
            int c = chiave.compareTo(x.chiave);
            if (c == 0) return conf;
            x = c < 0 ? x.sx : x.dx;
        }
        return 0;
    }

    /** Tutte le chiavi in ordine alfabetico (visita in-order). */
    public List<String> inOrdine() {
        List<String> out = new ArrayList<>(n);
        inOrdine(radice, out);
        return out;
    }

    private static void inOrdine(Nodo x, List<String> out) {
        if (x == null) return;
        inOrdine(x.sx, out);
        out.add(x.chiave);
        inOrdine(x.dx, out);
    }

    /** Chiavi che iniziano con il prefisso, in ordine alfabetico. Non visita i sottoalberi fuori intervallo. */
    public List<String> conPrefisso(String prefisso) {
        List<String> out = new ArrayList<>();
        conPrefisso(radice, prefisso, out);
        return out;
    }

    private static void conPrefisso(Nodo x, String p, List<String> out) {
        if (x == null) return;
        boolean ok = x.chiave.startsWith(p);
        // a sinistra solo se x e' >= p (altrimenti tutto il sottoalbero sinistro e' < x < p);
        // a destra solo se x e' <= p oppure ha il prefisso (altrimenti x > p e senza prefisso: a destra c'e' di piu')
        int c = x.chiave.compareTo(p);
        if (c >= 0) conPrefisso(x.sx, p, out);
        if (ok) out.add(x.chiave);
        if (c < 0 || ok) conPrefisso(x.dx, p, out);
    }
}
