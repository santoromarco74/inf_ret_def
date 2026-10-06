package ir;

import java.util.Arrays;

/**
 * Lista di postings (docId crescenti) con skip pointers.
 * Passo degli skip = radice quadrata della lunghezza: dalla posizione i (multiplo del passo)
 * si puo' saltare a i + passo. Serve ad accelerare l'intersezione di una lista corta con una lunga.
 */
public final class PostingList {
    public static final PostingList VUOTA = new PostingList(new int[0]);

    private final int[] ids;
    private final int passo;

    public PostingList(int[] ids) {
        this.ids = ids;
        this.passo = Math.max(1, (int) Math.sqrt(ids.length));
    }

    public int size() { return ids.length; }

    public int get(int i) { return ids[i]; }

    public int[] toArray() { return ids.clone(); }

    private boolean haSkip(int i) { return i % passo == 0 && i + passo < ids.length; }

    /** Intersezione con skip pointers. {@code confronti} (opzionale, lunghezza 1) conta i confronti fra docId. */
    public static PostingList intersect(PostingList a, PostingList b, long[] confronti) {
        int[] out = new int[Math.min(a.ids.length, b.ids.length)];
        int n = 0, i = 0, j = 0;
        long c = 0;
        while (i < a.ids.length && j < b.ids.length) {
            c++;
            int x = a.ids[i], y = b.ids[j];
            if (x == y) { out[n++] = x; i++; j++; }
            else if (x < y) {
                if (a.haSkip(i) && a.ids[i + a.passo] <= y) { do i += a.passo; while (a.haSkip(i) && a.ids[i + a.passo] <= y); }
                else i++;
            } else {
                if (b.haSkip(j) && b.ids[j + b.passo] <= x) { do j += b.passo; while (b.haSkip(j) && b.ids[j + b.passo] <= x); }
                else j++;
            }
        }
        if (confronti != null) confronti[0] += c;
        return new PostingList(Arrays.copyOf(out, n));
    }

    /** Stessa intersezione senza skip (merge lineare): riferimento per verificare risultati e confronti. */
    public static PostingList intersectLineare(PostingList a, PostingList b, long[] confronti) {
        int[] out = new int[Math.min(a.ids.length, b.ids.length)];
        int n = 0, i = 0, j = 0;
        long c = 0;
        while (i < a.ids.length && j < b.ids.length) {
            c++;
            int x = a.ids[i], y = b.ids[j];
            if (x == y) { out[n++] = x; i++; j++; }
            else if (x < y) i++;
            else j++;
        }
        if (confronti != null) confronti[0] += c;
        return new PostingList(Arrays.copyOf(out, n));
    }

    /** Unione (OR) di due liste ordinate. */
    public static PostingList union(PostingList a, PostingList b) {
        int[] out = new int[a.ids.length + b.ids.length];
        int n = 0, i = 0, j = 0;
        while (i < a.ids.length || j < b.ids.length) {
            if (j == b.ids.length || (i < a.ids.length && a.ids[i] < b.ids[j])) out[n++] = a.ids[i++];
            else if (i == a.ids.length || b.ids[j] < a.ids[i]) out[n++] = b.ids[j++];
            else { out[n++] = a.ids[i]; i++; j++; }
        }
        return new PostingList(Arrays.copyOf(out, n));
    }
}
