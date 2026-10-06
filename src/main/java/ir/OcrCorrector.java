package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Correzione OCR mirata sui termini del dizionario.
 * Si correggono solo scambi fra caratteri che l'OCR confonde davvero (tabella CONFUSIONI, ricavata dai
 * dati: stesse righe articolo, cioe' stesso codice, lette in modo diverso in bolle diverse) e solo verso un
 * termine che esiste gia' nel dizionario con piu' documenti. Niente correzioni "a distanza di edit":
 * i numeri che differiscono davvero (4GB/8GB) non vengono mai toccati.
 *
 * Prudenza: si toccano solo termini di almeno MIN_LUNGHEZZA caratteri e non fatti di sole cifre
 * (quantita', misure: troppo ambigui, un errore li trasformerebbe in un'altra parola valida).
 *
 * Per ogni termine t: si generano le varianti con al piu' MAX_SOSTITUZIONI scambi confusabili; fra quelle
 * presenti nel dizionario con df maggiore di df(t) si sceglie quella con df massimo (a parita', ordine alfabetico).
 */
public final class OcrCorrector {
    /** Coppie (carattere letto, carattere probabile) in minuscolo; la relazione e' simmetrica. */
    private static final String[] CONFUSIONI = {"0o", "1i", "5s", "6g", "8b", "7t"};
    private static final int MAX_SOSTITUZIONI = 2;
    private static final int MIN_LUNGHEZZA = 4;

    private OcrCorrector() {}

    private static char[] alternative(char c) {
        List<Character> out = new ArrayList<>();
        for (String p : CONFUSIONI) {
            if (p.charAt(0) == c) out.add(p.charAt(1));
            else if (p.charAt(1) == c) out.add(p.charAt(0));
        }
        char[] r = new char[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /** Mappa termine errato -> termine corretto (solo i termini che cambiano). */
    public static Map<String, String> calcola(InvertedIndex ix) {
        Map<String, String> out = new TreeMap<>();
        for (String t : ix.terms()) {
            if (t.length() < MIN_LUNGHEZZA || t.chars().allMatch(Character::isDigit)) continue;
            int df = ix.postings(t).size();
            Map<String, Integer> varianti = new HashMap<>();
            genera(t.toCharArray(), 0, 0, ix, varianti);
            String migliore = null;
            int dfMigliore = df;
            for (Map.Entry<String, Integer> e : varianti.entrySet()) {
                int d = e.getValue();
                if (d > dfMigliore || (d == dfMigliore && migliore != null && e.getKey().compareTo(migliore) < 0)) {
                    migliore = e.getKey();
                    dfMigliore = d;
                }
            }
            if (migliore != null) out.put(t, migliore);
        }
        return out;
    }

    private static void genera(char[] t, int pos, int fatte, InvertedIndex ix, Map<String, Integer> out) {
        if (pos == t.length) {
            if (fatte > 0) {
                String v = new String(t);
                if (ix.contains(v)) out.put(v, ix.postings(v).size());
            }
            return;
        }
        genera(t, pos + 1, fatte, ix, out);
        if (fatte < MAX_SOSTITUZIONI) {
            char orig = t[pos];
            for (char alt : alternative(orig)) {
                t[pos] = alt;
                genera(t, pos + 1, fatte + 1, ix, out);
            }
            t[pos] = orig;
        }
    }

    /** Ricostruisce l'indice applicando le correzioni ai termini di ogni documento. */
    public static InvertedIndex applica(InvertedIndex ix, Map<String, String> correzioni) {
        InvertedIndex nuovo = new InvertedIndex();
        for (int d = 0; d < ix.size(); d++) nuovo.add(ix.doc(d), correzioni);
        return nuovo;
    }
}
