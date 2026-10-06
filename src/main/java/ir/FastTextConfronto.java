package ir;

import com.github.jfasttext.JFastText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * MODELLO DI CONFRONTO CON LIBRERIA DI TERZI: fastText (Facebook) tramite il wrapper Java JFastText
 * (com.github.vinhkhuc:jfasttext, JNI con libreria nativa inclusa). e' codice di terzi:
 * addestramento del modello e calcolo dei vettori sono della libreria.
 *
 * Il codice del progetto contiene solo l'uso come motore di espansione dei termini: si addestra un modello skipgram con n-grammi
 * di carattere (3-6) sul testo del corpus, ogni termine del dizionario ha un vettore (anche i termini mai visti
 * ne ricevono uno dai loro n-grammi), una parola della query si espande nei termini del dizionario a coseno piu'
 * alto, poi si usano gli stessi postings e la stessa intersezione del resto del sistema.
 * Parametri fissati a priori (non ottimizzati sul benchmark): dim 50, minn 3, maxn 6, epoch 50, 1 thread;
 * espansione = al piu' 5 vicini con coseno >= 0.9. Non supporta i caratteri jolly.
 */
public final class FastTextConfronto {
    private static final int VICINI = 5;
    private static final double SOGLIA_COSENO = 0.9;

    private final InvertedIndex index;
    private final JFastText ft = new JFastText();
    private final List<String> termini;
    private final float[][] vettori;

    public FastTextConfronto(InvertedIndex index) throws IOException {
        this.index = index;
        Path dir = Files.createTempDirectory("fasttext");
        Path testo = dir.resolve("train.txt");
        StringBuilder sb = new StringBuilder();
        for (int d = 0; d < index.size(); d++) sb.append(String.join(" ", Tokenizer.tokenize(index.doc(d)))).append('\n');
        Files.writeString(testo, sb.toString());
        ft.runCmd(new String[]{"skipgram", "-input", testo.toString(), "-output", dir.resolve("modello").toString(),
                "-dim", "50", "-minn", "3", "-maxn", "6", "-minCount", "1", "-epoch", "50", "-thread", "1", "-verbose", "0"});
        ft.loadModel(dir.resolve("modello.bin").toString());
        termini = index.terms();
        vettori = new float[termini.size()][];
        for (int i = 0; i < termini.size(); i++) vettori[i] = normalizzato(ft.getVector(termini.get(i)));
    }

    private static float[] normalizzato(List<Float> v) {
        float[] x = new float[v.size()];
        double n = 0;
        for (int i = 0; i < x.length; i++) { x[i] = v.get(i); n += x[i] * x[i]; }
        n = Math.sqrt(n) + 1e-12;
        for (int i = 0; i < x.length; i++) x[i] /= (float) n;
        return x;
    }

    /** Termini del dizionario a cui la parola viene espansa (vicini per coseno nello spazio di fastText). */
    public List<String> espandi(String parola) {
        float[] q = normalizzato(ft.getVector(parola));
        double[] cos = new double[termini.size()];
        List<Integer> ordine = new ArrayList<>();
        for (int i = 0; i < termini.size(); i++) {
            double d = 0;
            for (int k = 0; k < q.length; k++) d += q[k] * vettori[i][k];
            cos[i] = d;
            ordine.add(i);
        }
        ordine.sort((a, b) -> Double.compare(cos[b], cos[a]));
        List<String> out = new ArrayList<>();
        for (int i : ordine.subList(0, Math.min(VICINI, ordine.size()))) if (cos[i] >= SOGLIA_COSENO) out.add(termini.get(i));
        return out;
    }

    public List<Integer> search(String query) {
        PostingList result = null;
        for (String parola : Tokenizer.tokenize(query)) {
            PostingList u = PostingList.VUOTA;
            for (String t : espandi(parola)) u = PostingList.union(u, index.postings(t));
            result = result == null ? u : PostingList.intersect(result, u, null);
        }
        List<Integer> out = new ArrayList<>();
        if (result != null) for (int i = 0; i < result.size(); i++) out.add(result.get(i));
        return out;
    }
}
