package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Valuta OcrCorrector con una verita' di riferimento ricavata dal corpus stesso: righe con lo stesso codice
 * articolo e descrizione di pari lunghezza in token; dove due letture dello stesso token (stessa lunghezza)
 * differiscono, la lettura piu' frequente fra le righe di quel codice e' presa come corretta.
 * Uso: java -cp target/classes ir.ValutaCorrezione
 */
public final class ValutaCorrezione {
    public static void main(String[] args) throws IOException {
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        InvertedIndex ix = new InvertedIndex();
        Map<String, List<String>> perCodice = new HashMap<>();
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            ix.add(c[4] + " " + c[5]);
            perCodice.computeIfAbsent(c[4], k -> new ArrayList<>()).add(c[5]);
        }

        // verita': token errato -> token giusto (solo token con lettere, lunghezza uguale: i numeri/quantita' sono ignorati)
        Map<String, String> verita = new HashMap<>();
        for (List<String> ds : perCodice.values()) {
            if (ds.size() < 2) continue;
            Map<String, Integer> freq = new HashMap<>();
            for (String d : ds) freq.merge(d, 1, Integer::sum);
            String rif = freq.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            List<String> tr = Tokenizer.tokenize(rif);
            for (String d : ds) {
                List<String> td = Tokenizer.tokenize(d);
                if (td.size() != tr.size()) continue;
                for (int i = 0; i < tr.size(); i++) {
                    String a = tr.get(i), b = td.get(i);
                    if (!a.equals(b) && a.length() == b.length() && b.matches(".*[a-z].*") && a.matches(".*[a-z].*")
                            && a.length() > 3 && !a.matches("[a-z]+") == !b.matches("[a-z]+")) {
                        verita.put(b, a);
                    }
                }
            }
        }

        Map<String, String> corr = OcrCorrector.calcola(ix);
        int giuste = 0, sbagliate = 0, nonVerificabili = 0;
        for (Map.Entry<String, String> e : corr.entrySet()) {
            String v = verita.get(e.getKey());
            if (v == null) nonVerificabili++;
            else if (v.equals(e.getValue())) giuste++;
            else sbagliate++;
        }
        int recuperati = 0;
        for (Map.Entry<String, String> e : verita.entrySet()) if (e.getValue().equals(corr.get(e.getKey()))) recuperati++;

        System.out.printf("termini nel dizionario: %d%n", ix.terms().size());
        System.out.printf("correzioni proposte: %d  (giuste %d, sbagliate %d, non verificabili %d)%n",
                corr.size(), giuste, sbagliate, nonVerificabili);
        System.out.printf("errori noti (stesso codice, lettura diversa): %d  recuperati: %d%n", verita.size(), recuperati);
        System.out.println("esempi:");
        corr.entrySet().stream().limit(25).forEach(e -> System.out.println("  " + e.getKey() + " -> " + e.getValue()
                + (verita.containsKey(e.getKey()) ? (verita.get(e.getKey()).equals(e.getValue()) ? "  [ok]" : "  [SBAGLIATA, giusto: " + verita.get(e.getKey()) + "]") : "")));
    }
}
