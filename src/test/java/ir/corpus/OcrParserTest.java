package ir.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OcrParserTest {
    private static final String TESTO = String.join("\n",
            "DESTINATAR 006220. Pg 1/1 03/02/2026 Vendita",
            "001519 - 078 LUOGO DI DESTINAZION",
            "20025 LEGNANO CORSO RICCI, 211R",
            "Rif. citato in fatt. VEND 006893 del 03/02/2026",
            "937518 XIAO BHR9917GL SMART WATCH 1.47\" BT SMART BAND 9 ACTIVE PINK 2",
            "949834 i BRON AMICOUNICOPIUBLACK CELL. QB 2\" D.SIM BT TASTO SOS 1.3MP+ 6",
            "|. | 944328 JBL JBLGOES2BLUEU MINI SPEAKER 2 BLUE 2",
            "962849 ZIE. BLADEAS5BLACKBUDS2BUNDLE SMARTP.6.75\"4GB 128GB A55S. i _. 6",
            "17100 SAVONA SV");

    @Test
    void intestazione() {
        OcrParser.Documento d = OcrParser.parse(TESTO);
        assertEquals("006220", d.numero());
        assertEquals("03/02/2026", d.data());
    }

    @Test
    void righeArticoloConRumore() {
        OcrParser.Documento d = OcrParser.parse(TESTO);
        assertEquals(4, d.articoli().size());
        assertEquals("937518", d.articoli().get(0).codice());
        assertEquals("BRON AMICOUNICOPIUBLACK CELL. QB 2\" D.SIM BT TASTO SOS 1.3MP+ 6", d.articoli().get(1).descrizione());
        assertEquals("944328", d.articoli().get(2).codice());
        assertEquals("ZIE BLADEAS5BLACKBUDS2BUNDLE SMARTP.6.75\"4GB 128GB A55S. 6", d.articoli().get(3).descrizione());
    }

    @Test
    void righeScartateClassificate() {
        OcrParser.Documento d = OcrParser.parse(TESTO);
        assertEquals(5, d.escluse().size());
        assertEquals("luogo_destinazione", d.escluse().get(1).motivo());
        assertEquals("indirizzo", d.escluse().get(2).motivo());
        assertEquals("riferimento_fattura_ordine", d.escluse().get(3).motivo());
    }
}
