package ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Indice invertito. Dizionario: {@link TabellaHash} termine -> voce (ricerca di un termine in tempo medio costante)
 * e {@link AlberoBinarioRicerca} dei termini (ordine alfabetico). Postings: docId crescenti, senza duplicati, con skip pointers.
 * I docId sono assegnati in ordine di inserimento, quindi le liste restano ordinate per costruzione.
 */
public class InvertedIndex implements Indice {
    /** Voce del dizionario: docId e tf in parallelo; la PostingList e' costruita al primo uso e invalidata da add. */
    private static final class Voce {
        final List<Integer> docs = new ArrayList<>();
        final List<Integer> tf = new ArrayList<>();
        PostingList congelata;
    }

    private final TabellaHash<Voce> dizionario = new TabellaHash<>();
    private final AlberoBinarioRicerca ordinati = new AlberoBinarioRicerca();
    private final List<String> docs = new ArrayList<>();
    private final List<Integer> lunghezze = new ArrayList<>();
    private long sommaLunghezze = 0;

    /** Aggiunge un documento (una riga articolo) e restituisce il suo docId. */
    public int add(String text) {
        return add(text, Map.of());
    }

    /** Come {@link #add(String)}, ma ogni termine passa prima per la mappa di correzione (es. OcrCorrector). */
    public int add(String text, Map<String, String> correzioni) {
        int id = docs.size();
        docs.add(text);
        Map<String, Integer> conteggio = new java.util.LinkedHashMap<>();
        int n = 0;
        for (String t0 : Tokenizer.tokenize(text)) {
            conteggio.merge(correzioni.getOrDefault(t0, t0), 1, Integer::sum);
            n++;
        }
        for (Map.Entry<String, Integer> e : conteggio.entrySet()) {
            Voce v = dizionario.get(e.getKey());
            if (v == null) { v = new Voce(); dizionario.put(e.getKey(), v); ordinati.inserisci(e.getKey()); }
            v.docs.add(id);
            v.tf.add(e.getValue());
            v.congelata = null;
        }
        lunghezze.add(n);
        sommaLunghezze += n;
        return id;
    }

    @Override
    public String doc(int id) { return docs.get(id); }

    @Override
    public int size() { return docs.size(); }

    /** Termini del dizionario in ordine alfabetico. */
    @Override
    public List<String> terms() { return ordinati.inOrdine(); }

    /** Termini che iniziano con il prefisso, in ordine alfabetico (visita limitata dell'albero). */
    public List<String> terminiConPrefisso(String prefisso) { return ordinati.conPrefisso(prefisso); }

    public TabellaHash<?> tabella() { return dizionario; }

    public AlberoBinarioRicerca albero() { return ordinati; }

    @Override
    public boolean contains(String term) { return dizionario.contains(term); }

    @Override
    public PostingList postings(String term) {
        Voce v = dizionario.get(term);
        if (v == null) return PostingList.VUOTA;
        if (v.congelata == null) v.congelata = new PostingList(v.docs.stream().mapToInt(Integer::intValue).toArray());
        return v.congelata;
    }

    @Override
    public int[] tf(String term) {
        Voce v = dizionario.get(term);
        return v == null ? new int[0] : v.tf.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public int lunghezza(int docId) { return lunghezze.get(docId); }

    @Override
    public double lunghezzaMedia() { return docs.isEmpty() ? 0 : (double) sommaLunghezze / docs.size(); }

    /** AND di tutti i termini della query, con skip pointers. */
    public List<Integer> searchAnd(String query) {
        List<String> terms = Tokenizer.tokenize(query);
        if (terms.isEmpty()) return List.of();
        PostingList result = postings(terms.get(0));
        for (int i = 1; i < terms.size() && result.size() > 0; i++) {
            result = PostingList.intersect(result, postings(terms.get(i)), null);
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) out.add(result.get(i));
        return out;
    }
}
