package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Indice a k-grammi (trigrammi) sui TERMINI del dizionario.
 * Ogni termine e' racchiuso fra '$' ("vite" -> $vi, vit, ite, te$); ogni k-gramma punta alla lista
 * ordinata degli id dei termini che lo contengono. Due usi:
 *  - wildcard: "vi*e" -> k-grammi dei pezzi fissi ($vi, te$) -> intersezione -> filtro sul pattern;
 *  - fuzzy: k-grammi del termine errato -> conteggio dei k-grammi condivisi -> Jaccard -> Levenshtein.
 */
public final class KGramIndex {
    private static final int[] NESSUNO = new int[0];
    private final int k;
    private final List<String> termini;
    private final TabellaHash<int[]> lista = new TabellaHash<>();
    private final AlberoBinarioRicerca albero; // termini in ordine: pattern con prefisso fisso -> visita di un intervallo

    public KGramIndex(List<String> terminiOrdinati, int k) {
        this.k = k;
        this.termini = terminiOrdinati;
        this.albero = AlberoBinarioRicerca.daOrdinati(terminiOrdinati); // termini gia' ordinati: inserimento di mezzo per avere altezza minima
        TabellaHash<List<Integer>> tmp = new TabellaHash<>();
        for (int id = 0; id < termini.size(); id++) {
            for (String g : kgrams("$" + termini.get(id) + "$")) {
                List<Integer> l = tmp.computeIfAbsent(g, x -> new ArrayList<>());
                if (l.isEmpty() || l.get(l.size() - 1) != id) l.add(id);
            }
        }
        for (String g : tmp.chiavi()) lista.put(g, tmp.get(g).stream().mapToInt(Integer::intValue).toArray());
    }

    public int numeroKGrammi() { return lista.size(); }

    public TabellaHash<int[]> tabella() { return lista; }

    public AlberoBinarioRicerca albero() { return albero; }

    private List<String> kgrams(String s) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i + k <= s.length(); i++) out.add(s.substring(i, i + k));
        return out;
    }

    /** Termini del dizionario che corrispondono al pattern (con '*' = qualsiasi sequenza, anche vuota). */
    public List<String> wildcard(String pattern) {
        if (!pattern.startsWith("*") && pattern.contains("*")) { // prefisso fisso: intervallo nell'albero, poi filtro sul pattern
            String prefisso = pattern.substring(0, pattern.indexOf('*'));
            List<String> ris = new ArrayList<>();
            for (String t : albero.conPrefisso(prefisso)) if (corrisponde("$" + pattern + "$", "$" + t + "$")) ris.add(t);
            return ris;
        }
        String p = "$" + pattern + "$";
        String[] pezzi = p.split("\\*", -1);
        int[] candidati = null;
        for (String pezzo : pezzi) {
            for (String g : kgrams(pezzo)) {
                int[] l = lista.getOrDefault(g, NESSUNO);
                candidati = candidati == null ? l : intersect(candidati, l);
            }
        }
        List<String> out = new ArrayList<>();
        if (candidati == null) { // nessun pezzo lungo almeno k: scansione del dizionario
            for (String t : termini) if (corrisponde(p, "$" + t + "$")) out.add(t);
        } else {
            for (int id : candidati) if (corrisponde(p, "$" + termini.get(id) + "$")) out.add(termini.get(id));
        }
        return out;
    }

    /** Termini simili: Jaccard sui k-grammi >= sogliaJaccard e distanza di edit <= maxEdit. Ordinati per distanza. */
    public List<String> fuzzy(String termine, double sogliaJaccard, int maxEdit) {
        List<String> gq = new ArrayList<>(new TreeSet<>(kgrams("$" + termine + "$")));
        int[] comuni = new int[termini.size()]; // k-grammi condivisi con il termine errato, per id di termine
        for (String g : gq) for (int id : lista.getOrDefault(g, NESSUNO)) comuni[id]++;
        List<String> out = new ArrayList<>();
        Map<String, Integer> dist = new HashMap<>();
        for (int id = 0; id < comuni.length; id++) {
            if (comuni[id] == 0) continue;
            String t = termini.get(id);
            int nt = new TreeSet<>(kgrams("$" + t + "$")).size();
            double jaccard = (double) comuni[id] / (gq.size() + nt - comuni[id]);
            if (jaccard < sogliaJaccard) continue;
            int d = EditDistance.levenshtein(termine, t);
            if (d <= maxEdit) { out.add(t); dist.put(t, d); }
        }
        out.sort((x, y) -> dist.get(x) != dist.get(y).intValue() ? dist.get(x) - dist.get(y) : x.compareTo(y));
        return out;
    }

    private static int[] intersect(int[] a, int[] b) {
        int[] out = new int[Math.min(a.length, b.length)];
        int n = 0, i = 0, j = 0;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { out[n++] = a[i]; i++; j++; }
            else if (a[i] < b[j]) i++;
            else j++;
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /** Confronto pattern-testo con '*' (greedy sui pezzi, senza regex). */
    static boolean corrisponde(String pattern, String testo) {
        String[] pezzi = pattern.split("\\*", -1);
        if (!testo.startsWith(pezzi[0])) return false;
        int pos = pezzi[0].length();
        for (int i = 1; i < pezzi.length - 1; i++) {
            int t = testo.indexOf(pezzi[i], pos);
            if (t < 0) return false;
            pos = t + pezzi[i].length();
        }
        if (pezzi.length == 1) return testo.length() == pezzi[0].length();
        String ultimo = pezzi[pezzi.length - 1];
        return testo.length() - ultimo.length() >= pos && testo.endsWith(ultimo);
    }
}
