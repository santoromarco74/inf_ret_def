package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stampa gli esempi concreti citati nella relazione (postings, trigrammi, fuzzy, compressione, correzioni,
 * casi di ricerca), cosi' sono riproducibili: java -cp target/classes ir.Esempi
 */
public final class Esempi {
    static InvertedIndex base;
    static Map<String, List<Integer>> perCodice = new HashMap<>();

    public static void main(String[] args) throws IOException {
        base = new InvertedIndex();
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            int id = base.add(c[4] + " " + c[5]);
            perCodice.computeIfAbsent(c[4], k -> new ArrayList<>()).add(id);
        }
        Map<String, String> corr = OcrCorrector.calcola(base);
        InvertedIndex corretto = OcrCorrector.applica(base, corr);

        System.out.println("### postings");
        for (String t : new String[]{"friggitrice", "aria", "tv", "8gb", "pimer"}) {
            PostingList p = base.postings(t);
            System.out.printf("%s df=%d primi=%s%n", t, p.size(), java.util.Arrays.toString(java.util.Arrays.copyOf(p.toArray(), Math.min(8, p.size()))));
        }

        System.out.println("### skip");
        PostingList a = base.postings("aria"), b = base.postings("friggitrice");
        long[] cs = {0}, cl = {0};
        PostingList r1 = PostingList.intersect(a, b, cs), r2 = PostingList.intersectLineare(a, b, cl);
        System.out.printf("aria(df %d) AND friggitrice(df %d): risultati %d, confronti skip %d, lineare %d, passo skip aria=%d friggitrice=%d%n",
                a.size(), b.size(), r1.size(), cs[0], cl[0], (int) Math.sqrt(a.size()), (int) Math.sqrt(b.size()));

        for (String[] c : new String[][]{{"tv", "aria"}, {"8gb", "friggitrice"}, {"tv", "8gb"}}) {
            PostingList x = base.postings(c[0]), y = base.postings(c[1]);
            long[] s1 = {0}, l1 = {0};
            int n = PostingList.intersect(x, y, s1).size();
            PostingList.intersectLineare(x, y, l1);
            System.out.printf("%s(df %d) AND %s(df %d): risultati %d, confronti skip %d, lineare %d%n", c[0], x.size(), c[1], y.size(), n, s1[0], l1[0]);
        }

        System.out.println("### trigrammi");
        for (String t : new String[]{"friggitrice", "lava"}) System.out.println(t + " -> " + kgrams("$" + t + "$"));
        KGramIndex kg = new KGramIndex(base.terms(), 3);
        System.out.println("numero trigrammi distinti nel dizionario: " + kg.numeroKGrammi());
        for (String g : new String[]{"$la", "lav", "ava"}) {
            int n = 0;
            for (String t : base.terms()) if (kgrams("$" + t + "$").contains(g)) n++;
            System.out.println("termini con trigramma " + g + ": " + n);
        }
        for (String pat : new String[]{"lava*", "*frigo*", "sdcz5*", "*gb35"}) {
            System.out.println("wildcard " + pat + " -> " + kg.wildcard(pat));
        }

        System.out.println("### fuzzy");
        for (String q : new String[]{"frigitrice", "tasta", "lavatrise"}) {
            System.out.println("fuzzy " + q + " (jaccard>=0.2, edit<=" + (q.length() <= 4 ? 1 : 2) + ") -> " + dettaglioFuzzy(kg, q));
        }

        System.out.println("### compressione");
        PostingList p = base.postings("aria");
        int[] ids = p.toArray();
        int[] primi = java.util.Arrays.copyOf(ids, 12);
        StringBuilder gap = new StringBuilder();
        int prec = 0;
        for (int x : primi) { gap.append(x - prec).append(' '); prec = x; }
        byte[] vb = VByte.codificaPostings(primi);
        StringBuilder hex = new StringBuilder();
        for (byte x : vb) hex.append(String.format("%02X ", x));
        System.out.println("aria primi 12 docId: " + java.util.Arrays.toString(primi));
        System.out.println("gap: " + gap);
        System.out.println("VByte (" + vb.length + " byte vs " + 4 * primi.length + "): " + hex);
        int[] grandi = {300, 20000, 1000000};
        StringBuilder h2 = new StringBuilder();
        for (byte x : VByte.codificaPostings(grandi)) h2.append(String.format("%02X ", x));
        System.out.println("esempio 300,20000,1000000 -> gap 300,19700,980000 -> " + h2);

        System.out.println("### correzioni (termine errato -> corretto, df errato, df corretto)");
        for (Map.Entry<String, String> e : corr.entrySet()) {
            System.out.printf("%s -> %s  df %d -> %d%n", e.getKey(), e.getValue(), base.postings(e.getKey()).size(), base.postings(e.getValue()).size());
        }

        System.out.println("### ranking");
        long somma = 0, voci = 0, ripetute = 0;
        for (String t : base.terms()) for (int f : base.tf(t)) { somma += f; voci++; if (f > 1) ripetute++; }
        System.out.printf("token totali=%d, coppie termine-documento=%d, di cui con tf>1: %d (%.1f%%)%n", somma, voci, ripetute, 100.0 * ripetute / voci);
        for (String q : new String[]{"friggitrice aria", "lavatrice slim"}) {
            Searcher se = new Searcher(base, Searcher.ModoFuzzy.NO);
            List<String> termini = new ArrayList<>();
            for (String w : q.split(" ")) termini.addAll(se.espandi(w));
            System.out.println("query OR '" + q + "' (N=" + base.size() + ", lunghezza media=" + String.format("%.2f", base.lunghezzaMedia()) + ")");
            for (String t : termini) System.out.println("  termine " + t + ": df=" + base.postings(t).size() + ", idf BM25=" + String.format("%.3f", Math.log(1 + (base.size() - base.postings(t).size() + 0.5) / (base.postings(t).size() + 0.5))) + ", idf TF-IDF=" + String.format("%.3f", Math.log((double) base.size() / base.postings(t).size())));
            for (Ranker.Modello m : new Ranker.Modello[]{Ranker.Modello.TFIDF, Ranker.Modello.BM25}) {
                Map<Integer, Double> pt = new Ranker(base).punteggi(termini, m);
                List<Integer> ord = se.searchRanked(q, m, false);
                System.out.println("  " + m + " (primi 5 di " + ord.size() + "):");
                for (int id : ord.subList(0, Math.min(5, ord.size()))) System.out.printf("    %.3f  len=%d  %s%n", pt.get(id), base.lunghezza(id), base.doc(id));
            }
        }

        System.out.println("### casi di ricerca");
        caso("friggitrice aria", base);
        caso("lava*", base);
        caso("frigitrice", base);
        caso("tasta", base);
        // una query del benchmark in cui le righe sono lette diversamente
        for (Map.Entry<String, List<Integer>> e : perCodice.entrySet()) {
            if (e.getValue().size() < 3) continue;
            Map<String, Integer> freq = new HashMap<>();
            for (int id : e.getValue()) freq.merge(base.doc(id), 1, Integer::sum);
            String rif = freq.entrySet().stream().max((x, y) -> x.getValue().equals(y.getValue()) ? x.getKey().compareTo(y.getKey()) : x.getValue() - y.getValue()).get().getKey();
            List<String> t = Tokenizer.tokenize(rif);
            if (t.size() < 4 || t.get(2).length() < 6) continue;
            String q = t.get(2) + " " + t.get(3);
            Set<Integer> rel = new HashSet<>(e.getValue());
            Set<Integer> esatta = new HashSet<>(new Searcher(base, Searcher.ModoFuzzy.NO).search(q));
            Set<Integer> fz = new HashSet<>(new Searcher(base, Searcher.ModoFuzzy.SEMPRE).search(q));
            Set<Integer> oc = new HashSet<>(new Searcher(corretto, Searcher.ModoFuzzy.NO).search(q));
            if (esatta.size() < rel.size() && fz.containsAll(rel) && !oc.containsAll(rel) && fz.size() == rel.size()) {
                System.out.println("QUERY BENCHMARK " + q + " (codice " + e.getKey() + ", rilevanti " + rel.size() + ")");
                for (int id : rel) System.out.println("  rilevante " + id + (esatta.contains(id) ? " [esatta]" : " [persa dall'esatta]") + (fz.contains(id) ? " [fuzzy]" : "") + (oc.contains(id) ? " [corr]" : "") + ": " + base.doc(id));
                break;
            }
        }
        metricheQuery("gbbsj21dep combi", "972441");
        metricheQuery("mq10001p minipimer", "859531");
        // falso positivo del fuzzy sempre
        for (Map.Entry<String, List<Integer>> e : perCodice.entrySet()) {
            if (e.getValue().size() < 2) continue;
            String rif = base.doc(e.getValue().get(0));
            List<String> t = Tokenizer.tokenize(rif);
            if (t.size() < 4 || t.get(2).length() < 5) continue;
            String q = t.get(2) + " " + t.get(3);
            Set<Integer> rel = new HashSet<>(e.getValue());
            List<Integer> fz = new Searcher(base, Searcher.ModoFuzzy.SEMPRE).search(q);
            List<Integer> estranei = new ArrayList<>();
            for (int id : fz) if (!rel.contains(id)) estranei.add(id);
            if (estranei.size() >= 2 && estranei.size() <= 4) {
                System.out.println("FALSO POSITIVO fuzzy sempre, query " + q + " (codice " + e.getKey() + ")");
                for (int id : estranei) System.out.println("  estraneo " + id + ": " + base.doc(id));
                for (int id : rel) System.out.println("  atteso " + id + ": " + base.doc(id));
                break;
            }
        }
    }

    static void metriche(String nome, Set<Integer> trovati, Set<Integer> rel, int n) {
        int tp = 0;
        for (int d : trovati) if (rel.contains(d)) tp++;
        int fp = trovati.size() - tp, fn = rel.size() - tp;
        double p = trovati.isEmpty() ? 0 : (double) tp / trovati.size(), r = (double) tp / rel.size();
        double f = p + r == 0 ? 0 : 2 * p * r / (p + r);
        System.out.printf("  %s: TP=%d FP=%d FN=%d P=%.3f R=%.3f F1=%.3f acc=%.5f%n", nome, tp, fp, fn, p, r, f, (double) (n - fp - fn) / n);
    }

    static void metricheQuery(String q, String codice) {
        Set<Integer> rel = new HashSet<>(perCodice.get(codice));
        InvertedIndex corr = OcrCorrector.applica(base, OcrCorrector.calcola(base));
        System.out.println("METRICHE query '" + q + "' (codice " + codice + ", " + rel.size() + " rilevanti, N=" + base.size() + ")");
        metriche("esatta", new HashSet<>(new Searcher(base, Searcher.ModoFuzzy.NO).search(q)), rel, base.size());
        metriche("fuzzy sempre", new HashSet<>(new Searcher(base, Searcher.ModoFuzzy.SEMPRE).search(q)), rel, base.size());
        metriche("correzione OCR", new HashSet<>(new Searcher(corr, Searcher.ModoFuzzy.NO).search(q)), rel, base.size());
    }

    static void caso(String q, InvertedIndex ix) {
        List<Integer> res = new Searcher(ix, Searcher.ModoFuzzy.FALLBACK).search(q);
        System.out.println("query '" + q + "' -> " + res.size() + " risultati; primi:");
        for (int id : res.subList(0, Math.min(3, res.size()))) System.out.println("  " + id + ": " + ix.doc(id));
    }

    static List<String> kgrams(String s) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i + 3 <= s.length(); i++) out.add(s.substring(i, i + 3));
        return out;
    }

    static String dettaglioFuzzy(KGramIndex kg, String q) {
        List<String> res = kg.fuzzy(q, 0.2, q.length() <= 4 ? 1 : 2);
        StringBuilder sb = new StringBuilder();
        Set<String> gq = new HashSet<>(kgrams("$" + q + "$"));
        for (String t : res) {
            Set<String> gt = new HashSet<>(kgrams("$" + t + "$"));
            Set<String> inter = new HashSet<>(gq);
            inter.retainAll(gt);
            Set<String> uni = new HashSet<>(gq);
            uni.addAll(gt);
            sb.append(String.format("%s[J=%.2f,d=%d] ", t, (double) inter.size() / uni.size(), EditDistance.levenshtein(q, t)));
        }
        return sb.toString();
    }
}
