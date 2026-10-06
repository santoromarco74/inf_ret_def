package ir.corpus;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Passi 3-4 della pipeline: applica {@link OcrParser} a ogni file di data/ocr,
 * scrive data/corpus.tsv e data/righe_escluse.tsv (pulizia tracciata).
 * Un file senza nessuna riga articolo (layout di un altro fornitore) e' escluso per intero e registrato.
 *
 * Uso: CorpusBuilder [cartella_ocr] [corpus.tsv] [righe_escluse.tsv]
 */
public final class CorpusBuilder {
    public static void main(String[] args) throws IOException {
        Path in = Path.of(args.length > 0 ? args[0] : "data/ocr");
        Path outCorpus = Path.of(args.length > 1 ? args[1] : "data/corpus.tsv");
        Path outEscluse = Path.of(args.length > 2 ? args[2] : "data/righe_escluse.tsv");

        List<Path> files;
        try (Stream<Path> s = Files.list(in)) {
            files = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }

        int rigaId = 0, docConArticoli = 0, docScartati = 0, nEscluse = 0, senzaIntestazione = 0;
        try (PrintWriter corpus = new PrintWriter(outCorpus.toFile(), StandardCharsets.UTF_8);
             PrintWriter escluse = new PrintWriter(outEscluse.toFile(), StandardCharsets.UTF_8)) {
            corpus.println("riga_id\tdoc_id\tnumero\tdata\tcodice\tdescrizione");
            escluse.println("doc_id\tmotivo\ttesto");
            for (Path f : files) {
                String docId = f.getFileName().toString().replaceFirst("\\.txt$", "");
                OcrParser.Documento d = OcrParser.parse(Files.readString(f, StandardCharsets.UTF_8));
                if (d.articoli().isEmpty()) {
                    docScartati++;
                    escluse.println(docId + "\tdocumento_senza_righe_articolo\t(" + d.escluse().size() + " righe, layout non gestito)");
                    continue;
                }
                docConArticoli++;
                if (d.numero().isEmpty()) senzaIntestazione++;
                for (OcrParser.Articolo a : d.articoli()) {
                    corpus.println(++rigaId + "\t" + docId + "\t" + d.numero() + "\t" + d.data()
                            + "\t" + a.codice() + "\t" + a.descrizione());
                }
                for (OcrParser.Esclusa e : d.escluse()) {
                    nEscluse++;
                    escluse.println(docId + "\t" + e.motivo() + "\t" + e.testo());
                }
            }
        }
        System.out.printf("documenti con articoli: %d (senza numero/data: %d), scartati interi: %d, righe articolo: %d, righe escluse: %d%n",
                docConArticoli, senzaIntestazione, docScartati, rigaId, nEscluse);
    }
}
