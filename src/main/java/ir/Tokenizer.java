package ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tokenizzazione: minuscolo, separazione sui caratteri non alfanumerici. */
public final class Tokenizer {
    private Tokenizer() {}

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ITALIAN).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cur.append(c);
            } else if (cur.length() > 0) {
                tokens.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens;
    }
}
