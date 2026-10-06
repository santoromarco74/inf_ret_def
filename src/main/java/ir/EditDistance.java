package ir;

/** Distanza di Levenshtein (programmazione dinamica a due righe). */
public final class EditDistance {
    private EditDistance() {}

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int sost = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(sost, Math.min(prev[j] + 1, cur[j - 1] + 1));
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
