package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistenza dell'indice: costruisce l'indice dal corpus, lo salva compresso su file e misura
 * quanto costa ricostruirlo in memoria rispetto a ricaricarlo.
 *
 *   java -cp target/classes ir.Persistenza                      salva data/indice.bin e data/indice_ocr.bin
 *   java -cp target/classes ir.Persistenza tempi build|carica N  tempo di avvio (mediana e minimo su N ripetizioni, in ms)
 */
public final class Persistenza {
    static final Path FILE = Path.of("data/indice.bin");
    static final Path FILE_OCR = Path.of("data/indice_ocr.bin");

    static InvertedIndex costruisci() throws IOException {
        InvertedIndex ix = new InvertedIndex();
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            ix.add(c[4] + " " + c[5]);
        }
        return ix;
    }

    /** Carica l'indice da file (con o senza correzione OCR) se esiste, altrimenti lo ricostruisce dal corpus. */
    static Indice apri(boolean ocr, boolean daFile) throws IOException {
        if (daFile) {
            Path f = ocr ? FILE_OCR : FILE;
            if (!Files.exists(f)) throw new IOException("manca " + f + ": lancia prima  java -cp target/classes ir.Persistenza");
            return CompressedIndex.carica(f);
        }
        InvertedIndex ix = costruisci();
        return ocr ? OcrCorrector.applica(ix, OcrCorrector.calcola(ix)) : ix;
    }

    public static void main(String[] args) throws IOException {
        if (args.length >= 3 && args[0].equals("tempi")) {
            tempi(args[1].equals("carica"), Integer.parseInt(args[2]));
            return;
        }
        InvertedIndex base = costruisci();
        InvertedIndex corretto = OcrCorrector.applica(base, OcrCorrector.calcola(base));
        new CompressedIndex(base).salva(FILE);
        new CompressedIndex(corretto).salva(FILE_OCR);
        System.out.printf("corpus.tsv:       %d byte%n", Files.size(Path.of("data/corpus.tsv")));
        System.out.printf("indice.bin:       %d byte (indice normale)%n", Files.size(FILE));
        System.out.printf("indice_ocr.bin:   %d byte (con correzione OCR)%n", Files.size(FILE_OCR));
        CompressedIndex c = CompressedIndex.carica(FILE);
        boolean uguale = c.terms().equals(base.terms()) && c.size() == base.size();
        for (String t : base.terms()) uguale &= java.util.Arrays.equals(c.postings(t).toArray(), base.postings(t).toArray());
        System.out.println("ricaricato == originale: " + uguale);
    }

    /** Tempo per essere pronti a cercare: indice + indice a trigrammi (costruito dal Searcher in entrambi i casi). */
    static void tempi(boolean daFile, int n) throws IOException {
        List<Double> ms = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long t0 = System.nanoTime();
            Searcher s = new Searcher(apri(false, daFile), Searcher.ModoFuzzy.FALLBACK);
            ms.add((System.nanoTime() - t0) / 1e6);
            if (s.search("lava*").isEmpty()) throw new IllegalStateException("ricerca di controllo vuota");
        }
        double prima = ms.get(0);
        Collections.sort(ms);
        System.out.printf("%s, %d ripetizioni: prima %.1f ms, mediana %.1f ms, minimo %.1f ms%n",
                daFile ? "caricamento da file" : "costruzione da corpus.tsv", n, prima, ms.get(n / 2), ms.get(0));
    }
}
