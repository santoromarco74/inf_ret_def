package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Misura lo spazio prima/dopo la compressione sul corpus reale: java -cp target/classes ir.Compressione */
public final class Compressione {
    public static void main(String[] args) throws IOException {
        InvertedIndex ix = new InvertedIndex();
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            ix.add(c[4] + " " + c[5]);
        }
        CompressedIndex ci = new CompressedIndex(ix);
        System.out.printf("documenti %d, termini %d%n", ix.size(), ix.terms().size());
        System.out.printf("dizionario: %d byte (non compresso)%n", ci.dizionarioByte());
        System.out.printf("postings:   %d -> %d byte (%.0f%%)%n", ci.postingsNonCompressi(), ci.postingsCompressi(),
                100.0 * ci.postingsCompressi() / ci.postingsNonCompressi());
    }
}
