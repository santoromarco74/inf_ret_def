package ir;

import java.util.List;

/** Vista di sola lettura di un indice: la usa {@link Searcher}, la implementano InvertedIndex (in memoria) e CompressedIndex (da file). */
public interface Indice {
    List<String> terms();

    boolean contains(String term);

    PostingList postings(String term);

    String doc(int id);

    int size();

    /** Frequenze del termine, nello stesso ordine dei suoi postings (tf[i] = occorrenze nel documento postings(term)[i]). */
    int[] tf(String term);

    /** Numero di token del documento (dopo le eventuali correzioni). */
    int lunghezza(int docId);

    double lunghezzaMedia();
}
