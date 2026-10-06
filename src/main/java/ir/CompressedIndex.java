package ir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Versione compressa e a sola lettura di un InvertedIndex: dizionario ordinato (ricerca binaria),
 * postings come gap + VByte in un unico array, frequenze dei termini (tf) come VByte, lunghezze dei documenti. {@link #postings} decodifica al volo e restituisce una PostingList.
 * Si puo' salvare su file e ricaricare ({@link #salva}, {@link #carica}): e' l'indice persistente del sistema.
 * Nel file ci sono anche i testi dei documenti (non compressi), per poter mostrare i risultati.
 * L'indice a trigrammi non e' salvato: lo ricostruisce il Searcher dai termini a ogni avvio.
 */
public final class CompressedIndex implements Indice {
    private static final int MAGIC = 0x49524958; // "IRIX"
    private static final int VERSIONE = 3;

    private final DizionarioOrdinato dizionario;
    private final byte[] postings;
    private final int[] offset; // offset[id]..offset[id+1] = postings del termine id
    private final List<String> docs;
    private final byte[] frequenze;   // tf di tutti i postings, VByte, nello stesso ordine di `postings`
    private final int[] offsetTf;     // offsetTf[id]..offsetTf[id+1] = tf del termine id
    private final int[] lunghezze;    // token per documento
    private final double lunghezzaMedia;

    private final long byteNonCompressi;

    public CompressedIndex(InvertedIndex ix) {
        List<String> termini = ix.terms();
        dizionario = new DizionarioOrdinato(termini);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream outTf = new ByteArrayOutputStream();
        offset = new int[termini.size() + 1];
        offsetTf = new int[termini.size() + 1];
        long raw = 0;
        for (int i = 0; i < termini.size(); i++) {
            offset[i] = out.size();
            offsetTf[i] = outTf.size();
            int[] p = ix.postings(termini.get(i)).toArray();
            out.writeBytes(VByte.codificaPostings(p));
            outTf.writeBytes(VByte.codificaInteri(ix.tf(termini.get(i))));
            raw += 4L * p.length;
        }
        offset[termini.size()] = out.size();
        offsetTf[termini.size()] = outTf.size();
        postings = out.toByteArray();
        frequenze = outTf.toByteArray();
        byteNonCompressi = raw;
        docs = new ArrayList<>(ix.size());
        lunghezze = new int[ix.size()];
        for (int d = 0; d < ix.size(); d++) {
            docs.add(ix.doc(d));
            lunghezze[d] = ix.lunghezza(d);
        }
        lunghezzaMedia = ix.lunghezzaMedia();
    }

    private CompressedIndex(DizionarioOrdinato dizionario, byte[] postings, int[] offset, List<String> docs,
                            byte[] frequenze, int[] offsetTf, int[] lunghezze) {
        this.dizionario = dizionario;
        this.postings = postings;
        this.offset = offset;
        this.docs = docs;
        this.frequenze = frequenze;
        this.offsetTf = offsetTf;
        this.lunghezze = lunghezze;
        long somma = 0;
        for (int l : lunghezze) somma += l;
        this.lunghezzaMedia = lunghezze.length == 0 ? 0 : (double) somma / lunghezze.length;
        this.byteNonCompressi = 0;
    }

    public void salva(Path file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSIONE);
            out.writeInt(docs.size());
            for (String d : docs) out.writeUTF(d);
            dizionario.scrivi(out);
            out.writeInt(postings.length);
            out.write(postings);
            out.writeInt(offset.length);
            for (int o : offset) out.writeInt(o);
            out.writeInt(frequenze.length);
            out.write(frequenze);
            for (int o : offsetTf) out.writeInt(o);
            for (int l : lunghezze) out.writeInt(l);
        }
    }

    public static CompressedIndex carica(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC) throw new IOException("non e' un indice di questo programma: " + file);
            if (in.readInt() != VERSIONE) throw new IOException("versione del file non supportata: " + file);
            List<String> docs = new ArrayList<>();
            for (int i = in.readInt(); i > 0; i--) docs.add(in.readUTF());
            DizionarioOrdinato d = DizionarioOrdinato.leggi(in);
            byte[] post = new byte[in.readInt()];
            in.readFully(post);
            int[] off = new int[in.readInt()];
            for (int i = 0; i < off.length; i++) off[i] = in.readInt();
            byte[] tf = new byte[in.readInt()];
            in.readFully(tf);
            int[] offTf = new int[off.length];
            for (int i = 0; i < offTf.length; i++) offTf[i] = in.readInt();
            int[] len = new int[docs.size()];
            for (int i = 0; i < len.length; i++) len[i] = in.readInt();
            return new CompressedIndex(d, post, off, docs, tf, offTf, len);
        }
    }

    @Override
    public boolean contains(String termine) { return dizionario.id(termine) >= 0; }

    @Override
    public List<String> terms() { return dizionario.terms(); }

    @Override
    public String doc(int id) { return docs.get(id); }

    @Override
    public int size() { return docs.size(); }

    @Override
    public PostingList postings(String termine) {
        int id = dizionario.id(termine);
        if (id < 0) return PostingList.VUOTA;
        return new PostingList(VByte.decodificaPostings(java.util.Arrays.copyOfRange(postings, offset[id], offset[id + 1])));
    }

    @Override
    public int[] tf(String termine) {
        int id = dizionario.id(termine);
        if (id < 0) return new int[0];
        return VByte.decodificaInteri(java.util.Arrays.copyOfRange(frequenze, offsetTf[id], offsetTf[id + 1]));
    }

    @Override
    public int lunghezza(int docId) { return lunghezze[docId]; }

    @Override
    public double lunghezzaMedia() { return lunghezzaMedia; }

    /** Dizionario (non compresso): per ogni termine la lunghezza in VByte e i byte UTF-8. */
    public long dizionarioByte() { return dizionario.byteOccupati(); }
    public long postingsCompressi() { return postings.length; }
    /** Postings non compressi: 4 byte per docId (0 se l'indice e' stato caricato da file). */
    public long postingsNonCompressi() { return byteNonCompressi; }
}
