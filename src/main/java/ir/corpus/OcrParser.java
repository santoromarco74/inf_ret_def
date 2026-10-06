package ir.corpus;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Passo 2 della pipeline (solo java.util.regex della JDK): testo OCR di una bolla
 * -> numero/data del documento + righe articolo. Ogni riga non articolo viene classificata e
 * restituita come "esclusa" con un motivo, mai buttata in silenzio.
 *
 * Gestisce un solo layout di fornitore: "codice MARCA descrizione... quantita".
 */
public final class OcrParser {

    /** Riga articolo: codice 5-7 cifre, marca 3-4 lettere maiuscole, resto = descrizione. Tollera rumore OCR davanti. */
    private static final Pattern ARTICOLO = Pattern.compile(
            "^\\W*(\\d{5,7})\\W{0,3}\\s+(?:\\W\\s+|[a-z]\\s+)?([A-Z]{3,4})\\.?\\s+(\\S.*)$");

    /** Intestazione: "006220 Pg 1/1 03/02/2026" (con punteggiatura OCR variabile tra i campi). */
    private static final Pattern INTESTAZIONE = Pattern.compile(
            "(\\d{6})\\W{0,3}\\s*Pg\\W{0,3}\\d\\s*/\\s*\\d\\W{0,3}\\s*(\\d{2}/\\d{2}/\\d{4})");

    /** Rumore OCR: token fatti solo di punteggiatura o singole lettere minuscole i/l (le descrizioni sono maiuscole). */
    private static final Pattern TOKEN_RUMORE = Pattern.compile("[\\W_]+|[il]");

    public record Articolo(String codice, String descrizione) {}

    public record Esclusa(String motivo, String testo) {}

    public record Documento(String numero, String data, List<Articolo> articoli, List<Esclusa> escluse) {}

    private OcrParser() {}

    public static Documento parse(String testoOcr) {
        String numero = "";
        String data = "";
        Matcher h = INTESTAZIONE.matcher(testoOcr);
        if (h.find()) {
            numero = h.group(1);
            data = h.group(2);
        }

        List<Articolo> articoli = new ArrayList<>();
        List<Esclusa> escluse = new ArrayList<>();
        for (String riga : testoOcr.split("[\\n\\f]")) {
            String r = riga.strip();
            if (r.isEmpty()) continue;
            Matcher m = ARTICOLO.matcher(r);
            if (m.matches()) {
                articoli.add(new Articolo(m.group(1), m.group(2) + " " + senzaRumore(m.group(3))));
            } else {
                escluse.add(new Esclusa(motivo(r), r));
            }
        }
        return new Documento(numero, data, articoli, escluse);
    }

    static String senzaRumore(String descrizione) {
        StringBuilder sb = new StringBuilder();
        for (String t : descrizione.split("\\s+")) {
            if (TOKEN_RUMORE.matcher(t).matches()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString();
    }

    static String motivo(String r) {
        String u = r.toUpperCase();
        if (u.contains("LUOGO DI DESTINAZ")) return "luogo_destinazione";
        if (INTESTAZIONE.matcher(r).find()) return "intestazione_documento";
        if (u.contains("RIF. CITATO") || u.contains("N.RO ORDINE")) return "riferimento_fattura_ordine";
        if (u.contains("LEGNANO") || u.contains("SAVONA") || u.contains("CORSO RICCI")) return "indirizzo";
        return "altro";
    }
}
