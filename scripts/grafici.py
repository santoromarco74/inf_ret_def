#!/usr/bin/env python3
"""Genera i grafici SVG della relazione da data/risultati_benchmark.txt (solo libreria standard di Python).
Strumento di contorno per la relazione, non fa parte del sistema di ricerca.
Uso: python3 scripts/grafici.py   ->  docs/img/*.svg
"""
import re
import sys

COL = {"P": "#2a6fbb", "R": "#e08a1e", "F1": "#3f9a6b"}


def leggi(path="data/risultati_benchmark.txt"):
    """Ritorna {sistema: {'tutte': (P,R,F1), 'varianti': (P,R,F1)}} per i sistemi con query esatte e fastText."""
    out, corrente = {}, None
    for riga in open(path, encoding="utf-8"):
        m = re.match(r"^(\S.*?)\s{2,}(tutte|con varianti)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+[\d.]+", riga)
        if m:
            corrente = m.group(1)
            out.setdefault(corrente, {})["tutte"] = tuple(float(x) for x in m.group(3, 4, 5))
            continue
        m = re.match(r"^\s+(con varianti)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+[\d.]+\s+\(n=", riga)
        if m and corrente:
            out[corrente]["varianti"] = tuple(float(x) for x in m.group(2, 3, 4))
    return out


def leggi_ranking(path="data/risultati_benchmark.txt"):
    """Ritorna [(sistema, (MAP, P@1, Rprec) tutte, (MAP, P@1, Rprec) con varianti)] dalla sezione ranking."""
    out, attivo, corrente = [], False, None
    for riga in open(path, encoding="utf-8"):
        if riga.startswith("ranking"):
            attivo = True
            continue
        if attivo and riga.startswith("confronti"):
            break
        if not attivo:
            continue
        m = re.match(r"^(\S.*?)\s{2,}(tutte)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+\(n=", riga)
        if m:
            corrente = [m.group(1), tuple(float(x) for x in m.group(3, 4, 5)), None]
            out.append(corrente)
            continue
        m = re.match(r"^\s+(con varianti)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+\(n=", riga)
        if m and corrente:
            corrente[2] = tuple(float(x) for x in m.group(2, 3, 4))
    return out


def barre_raggruppate(titolo, categorie, serie, nomi, colori, path, ymax=1.0, fmt="{:.2f}"):
    W, H, ml, mr, mt, mb = 820, 330, 50, 20, 45, 75
    pw, ph = W - ml - mr, H - mt - mb
    gw = pw / len(categorie)
    bw = gw * 0.78 / len(serie)
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" font-family="sans-serif" font-size="11">',
         f'<rect width="{W}" height="{H}" fill="white"/>',
         f'<text x="{W/2}" y="22" text-anchor="middle" font-size="14" font-weight="bold">{titolo}</text>']
    for i in range(0, 11, 2):
        y = mt + ph - ph * (i / 10)
        s.append(f'<line x1="{ml}" y1="{y:.1f}" x2="{W-mr}" y2="{y:.1f}" stroke="#ddd"/>')
        s.append(f'<text x="{ml-6}" y="{y+4:.1f}" text-anchor="end" fill="#555">{i/10*ymax:g}</text>')
    for c, cat in enumerate(categorie):
        x0 = ml + c * gw + gw * 0.11
        for k, ser in enumerate(serie):
            v = ser[c]
            h = ph * v / ymax
            x = x0 + k * bw
            s.append(f'<rect x="{x:.1f}" y="{mt+ph-h:.1f}" width="{bw-2:.1f}" height="{h:.1f}" fill="{colori[k]}"/>')
            s.append(f'<text x="{x+(bw-2)/2:.1f}" y="{mt+ph-h-3:.1f}" text-anchor="middle" font-size="9" fill="#333">{fmt.format(v + 1e-9).replace(".", ",")}</text>')
        for j, parte in enumerate(cat.split("|")):
            s.append(f'<text x="{ml+c*gw+gw/2:.1f}" y="{mt+ph+16+j*13}" text-anchor="middle" fill="#333">{parte}</text>')
    for k, n in enumerate(nomi):
        s.append(f'<rect x="{ml+k*90}" y="{H-22}" width="12" height="12" fill="{colori[k]}"/><text x="{ml+k*90+17}" y="{H-12}">{n}</text>')
    s.append(f'<line x1="{ml}" y1="{mt+ph}" x2="{W-mr}" y2="{mt+ph}" stroke="#333"/></svg>')
    open(path, "w", encoding="utf-8").write("\n".join(s))


def main():
    d = leggi()
    sistemi = [("esatta", "esatta"), ("fuzzy sempre", "fuzzy|sempre"), ("correzione OCR", "correzione|OCR"),
               ("correzione OCR + fuzzy sempre", "corr. OCR +|fuzzy sempre"),
               ("fastText (LIBRERIA, confronto)", "fastText|(libreria)")]
    mancanti = [n for n, _ in sistemi if n not in d]
    if mancanti:
        sys.exit("sistemi non trovati in risultati_benchmark.txt: %s" % mancanti)
    for chiave, titolo, nome in (("tutte", "Tutte le 214 query", "bench_tutte.svg"),
                                 ("varianti", "Le 65 query con almeno una riga letta diversamente", "bench_varianti.svg")):
        serie = [[d[n][chiave][i] for n, _ in sistemi] for i in range(3)]
        barre_raggruppate(titolo + ": precisione, richiamo, F1", [e for _, e in sistemi], serie,
                          ["P", "R", "F1"], [COL["P"], COL["R"], COL["F1"]], "docs/img/" + nome)
    rk = leggi_ranking()
    if not rk:
        sys.exit("sezione ranking non trovata in risultati_benchmark.txt")
    nomi = {"AND, nessun punteggio (docId)": "AND|docId", "AND + TF-IDF": "AND +|TF-IDF", "AND + BM25": "AND +|BM25",
            "OR, nessun punteggio (docId)": "OR|docId", "OR + TF-IDF": "OR +|TF-IDF", "OR + BM25": "OR +|BM25",
            "fuzzy sempre, AND + TF-IDF": "fuzzy, AND|+ TF-IDF", "fuzzy sempre, AND + BM25": "fuzzy, AND|+ BM25"}
    barre_raggruppate("Ranking (214 query esatte): MAP",
                      [nomi.get(n, n) for n, _, _ in rk],
                      [[t[0] for _, t, _ in rk], [v[0] for _, _, v in rk]],
                      ["tutte", "con varianti"], ["#2a6fbb", "#e08a1e"], "docs/img/ranking.svg")
    # confronti skip list e compressione: valori stampati da ir.Benchmark e ir.Compressione
    barre_raggruppate("Skip list: confronti fra docId nell'intersezione (214 query, in migliaia)",
                      ["modello AND parola", "modello AND '2'|(df 312)"], [[8.718, 45.824], [2.343, 6.552]],
                      ["lineare", "con skip"], ["#999999", "#2a6fbb"], "docs/img/skip.svg", ymax=50, fmt="{:.1f}")
    print("ok")


if __name__ == "__main__":
    main()
