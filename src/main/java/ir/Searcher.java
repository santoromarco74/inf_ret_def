package ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ricerca sopra un Indice (InvertedIndex o CompressedIndex) + KGramIndex. Query = parole separate da spazio, in AND.
 * Ogni parola: con '*' -> wildcard (OR dei termini espansi); altrimenti dipende dal {@link ModoFuzzy}:
 * NO = solo esatta; FALLBACK = fuzzy solo se la parola non e' nel dizionario; SEMPRE = sempre OR dei termini simili.
 */
public final class Searcher {
    private final Indice index;
    private final KGramIndex kgrams;
    private final ModoFuzzy modo;
    private final Ranker ranker;

    public enum ModoFuzzy { NO, FALLBACK, SEMPRE }

    public Searcher(Indice index, ModoFuzzy modo) {
        this.index = index;
        this.kgrams = new KGramIndex(index.terms(), 3);
        this.modo = modo;
        this.ranker = new Ranker(index);
    }

    public Searcher(Indice index, boolean fuzzyFallback) {
        this(index, fuzzyFallback ? ModoFuzzy.FALLBACK : ModoFuzzy.NO);
    }

    public List<Integer> search(String query) {
        PostingList result = null;
        for (String parola : query.toLowerCase(Locale.ITALIAN).trim().split("\\s+")) {
            if (parola.isEmpty()) continue;
            PostingList p = postingsParola(parola);
            result = result == null ? p : PostingList.intersect(result, p, null);
        }
        List<Integer> out = new ArrayList<>();
        if (result != null) for (int i = 0; i < result.size(); i++) out.add(result.get(i));
        return out;
    }

    /** Termini del dizionario a cui la parola viene espansa. */
    public List<String> espandi(String parola) {
        if (parola.contains("*")) return kgrams.wildcard(parola);
        if (modo == ModoFuzzy.SEMPRE || (modo == ModoFuzzy.FALLBACK && !index.contains(parola))) {
            return kgrams.fuzzy(parola, 0.2, parola.length() <= 4 ? 1 : 2);
        }
        return index.contains(parola) ? List.of(parola) : List.of();
    }

    private PostingList postingsParola(String parola) {
        PostingList u = PostingList.VUOTA;
        for (String t : espandi(parola)) u = PostingList.union(u, index.postings(t));
        return u;
    }

    /**
     * Ricerca con ordinamento per punteggio (decrescente; a parita' docId crescente).
     * @param and true = solo i documenti che contengono tutte le parole (come {@link #search});
     *            false = tutti i documenti che contengono almeno una parola (OR), ordinati per punteggio.
     */
    public List<Integer> searchRanked(String query, Ranker.Modello modello, boolean and) {
        return searchRanked(query, modello, and, Integer.MAX_VALUE);
    }

    /** Come sopra, ma solo i primi k risultati, estratti con un {@link HeapBinario} (O(n log k)). */
    public List<Integer> searchRanked(String query, Ranker.Modello modello, boolean and, int k) {
        List<Integer> out = new ArrayList<>();
        for (int d : classifica(query, modello, and, k).ids()) out.add(d);
        return out;
    }

    /** Un risultato con il suo punteggio (0 senza modello) e i termini del dizionario che hanno fatto trovare la riga. */
    public record Risultato(int docId, double punteggio, List<String> termini) {}

    /** I primi k risultati e il numero totale di righe trovate. */
    public record Risposta(int totale, List<Risultato> risultati) {}

    /**
     * Come {@link #searchRanked(String, Ranker.Modello, boolean, int)}, con in piu' punteggio e termini di ogni risultato.
     * I termini sono quelli a cui le parole della query sono espanse (wildcard, fuzzy) la cui posting list contiene la riga,
     * in ordine alfabetico. Si calcolano solo per i primi k.
     */
    public Risposta searchDettaglio(String query, Ranker.Modello modello, boolean and, int k) {
        Classifica c = classifica(query, modello, and, k);
        List<String> espansi = new ArrayList<>(new TreeSet<>(c.espansi()));
        List<Risultato> out = new ArrayList<>();
        for (int d : c.ids()) {
            List<String> termini = new ArrayList<>();
            for (String t : espansi) if (index.postings(t).contiene(d)) termini.add(t);
            out.add(new Risultato(d, c.punteggi().getOrDefault(d, 0.0), termini));
        }
        return new Risposta(c.totale(), out);
    }

    private record Classifica(int totale, int[] ids, Map<Integer, Double> punteggi, Set<String> espansi) {}

    private Classifica classifica(String query, Ranker.Modello modello, boolean and, int k) {
        List<String> parole = new ArrayList<>();
        for (String p : query.toLowerCase(Locale.ITALIAN).trim().split("\\s+")) if (!p.isEmpty()) parole.add(p);
        if (parole.isEmpty()) return new Classifica(0, new int[0], Map.of(), Set.of());
        Set<String> espansi = new HashSet<>();
        for (String p : parole) espansi.addAll(espandi(p));
        Map<Integer, Double> punteggi = ranker.punteggi(espansi, modello);
        Set<Integer> candidati = and ? new HashSet<>(search(query)) : punteggi.keySet();
        int[] ids = new int[candidati.size()];
        double[] pt = new double[ids.length];
        int i = 0;
        for (int d : candidati) { ids[i] = d; pt[i++] = punteggi.getOrDefault(d, 0.0); }
        return new Classifica(ids.length, HeapBinario.topK(ids, pt, k), punteggi, espansi);
    }
}
