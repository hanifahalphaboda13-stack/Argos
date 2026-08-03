#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Génère la présentation PowerPoint pour les superviseurs."""

from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

OUTPUT = "/home/hani/eclipse-workspace/PerforceLog/presentation_superviseurs.pptx"

BLEU_FONCE = RGBColor(0x1A, 0x36, 0x5D)
BLEU = RGBColor(0x2E, 0x5F, 0x9E)
GRIS = RGBColor(0x55, 0x55, 0x55)
BLANC = RGBColor(0xFF, 0xFF, 0xFF)
ACCENT = RGBColor(0xE8, 0x6C, 0x00)


def set_run(run, size=18, bold=False, color=None, font_name="Calibri"):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.name = font_name
    if color:
        run.font.color.rgb = color


def add_title_slide(prs, title, subtitle):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    bg.fill.solid()
    bg.fill.fore_color.rgb = BLEU_FONCE
    bg.line.fill.background()

    accent = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, Inches(4.8), prs.slide_width, Inches(0.08))
    accent.fill.solid()
    accent.fill.fore_color.rgb = ACCENT
    accent.line.fill.background()

    box = slide.shapes.add_textbox(Inches(0.6), Inches(1.4), Inches(8.8), Inches(2.2))
    tf = box.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    r = p.add_run()
    r.text = title
    set_run(r, 32, True, BLANC)
    p.alignment = PP_ALIGN.LEFT

    p2 = tf.add_paragraph()
    r2 = p2.add_run()
    r2.text = subtitle
    set_run(r2, 18, False, RGBColor(0xCC, 0xDD, 0xEE))
    p2.space_before = Pt(16)

    p3 = tf.add_paragraph()
    r3 = p3.add_run()
    r3.text = "Hanifah — UQAC — Juin 2026"
    set_run(r3, 14, False, RGBColor(0xAA, 0xBB, 0xCC))
    p3.space_before = Pt(28)

    p4 = tf.add_paragraph()
    r4 = p4.add_run()
    r4.text = "Superviseurs : Sylvain Hallé, Yannick"
    set_run(r4, 14, False, RGBColor(0xAA, 0xBB, 0xCC))
    p4.space_before = Pt(8)


def add_section_slide(prs, title):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    bg.fill.solid()
    bg.fill.fore_color.rgb = BLEU
    bg.line.fill.background()

    box = slide.shapes.add_textbox(Inches(0.8), Inches(2.6), Inches(8.4), Inches(1.2))
    tf = box.text_frame
    p = tf.paragraphs[0]
    r = p.add_run()
    r.text = title
    set_run(r, 36, True, BLANC)
    p.alignment = PP_ALIGN.LEFT


def add_content_slide(prs, title, bullets, sub_bullets=None):
    slide = prs.slides.add_slide(prs.slide_layouts[6])

    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, Inches(1.05))
    bar.fill.solid()
    bar.fill.fore_color.rgb = BLEU_FONCE
    bar.line.fill.background()

    title_box = slide.shapes.add_textbox(Inches(0.5), Inches(0.22), Inches(9), Inches(0.7))
    tp = title_box.text_frame.paragraphs[0]
    tr = tp.add_run()
    tr.text = title
    set_run(tr, 26, True, BLANC)

    body = slide.shapes.add_textbox(Inches(0.55), Inches(1.25), Inches(8.9), Inches(5.8))
    tf = body.text_frame
    tf.word_wrap = True

    sub_bullets = sub_bullets or {}
    for i, item in enumerate(bullets):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.level = 0
        p.space_after = Pt(10)
        r = p.add_run()
        r.text = item
        set_run(r, 17, False, GRIS)

        if item in sub_bullets:
            for sub in sub_bullets[item]:
                sp = tf.add_paragraph()
                sp.level = 1
                sp.space_after = Pt(4)
                sr = sp.add_run()
                sr.text = sub
                set_run(sr, 15, False, GRIS)


def add_two_column_slide(prs, title, left_title, left_items, right_title, right_items):
    slide = prs.slides.add_slide(prs.slide_layouts[6])

    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, Inches(1.05))
    bar.fill.solid()
    bar.fill.fore_color.rgb = BLEU_FONCE
    bar.line.fill.background()

    title_box = slide.shapes.add_textbox(Inches(0.5), Inches(0.22), Inches(9), Inches(0.7))
    tp = title_box.text_frame.paragraphs[0]
    tr = tp.add_run()
    tr.text = title
    set_run(tr, 26, True, BLANC)

    for col, col_title, items, x in [
        (0, left_title, left_items, 0.45),
        (1, right_title, right_items, 5.05),
    ]:
        hbox = slide.shapes.add_textbox(Inches(x), Inches(1.2), Inches(4.4), Inches(0.5))
        hp = hbox.text_frame.paragraphs[0]
        hr = hp.add_run()
        hr.text = col_title
        set_run(hr, 18, True, BLEU)

        bbox = slide.shapes.add_textbox(Inches(x), Inches(1.65), Inches(4.4), Inches(5.2))
        tf = bbox.text_frame
        tf.word_wrap = True
        for i, item in enumerate(items):
            p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
            p.space_after = Pt(8)
            r = p.add_run()
            r.text = "• " + item
            set_run(r, 15, False, GRIS)


def add_table_slide(prs, title, headers, rows):
    slide = prs.slides.add_slide(prs.slide_layouts[6])

    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, Inches(1.05))
    bar.fill.solid()
    bar.fill.fore_color.rgb = BLEU_FONCE
    bar.line.fill.background()

    title_box = slide.shapes.add_textbox(Inches(0.5), Inches(0.22), Inches(9), Inches(0.7))
    tp = title_box.text_frame.paragraphs[0]
    tr = tp.add_run()
    tr.text = title
    set_run(tr, 24, True, BLANC)

    cols = len(headers)
    table_shape = slide.shapes.add_table(len(rows) + 1, cols, Inches(0.4), Inches(1.2), Inches(9.2), Inches(0.45 * (len(rows) + 2)))
    table = table_shape.table

    for j, h in enumerate(headers):
        cell = table.cell(0, j)
        cell.text = h
        for p in cell.text_frame.paragraphs:
            for r in p.runs:
                set_run(r, 12, True, BLANC)
        cell.fill.solid()
        cell.fill.fore_color.rgb = BLEU

    for i, row in enumerate(rows):
        for j, val in enumerate(row):
            cell = table.cell(i + 1, j)
            cell.text = val
            for p in cell.text_frame.paragraphs:
                for r in p.runs:
                    set_run(r, 11, False, GRIS)
            if i % 2 == 0:
                cell.fill.solid()
                cell.fill.fore_color.rgb = RGBColor(0xF0, 0xF4, 0xF8)


def add_architecture_slide(prs):
    slide = prs.slides.add_slide(prs.slide_layouts[6])

    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, Inches(1.05))
    bar.fill.solid()
    bar.fill.fore_color.rgb = BLEU_FONCE
    bar.line.fill.background()

    title_box = slide.shapes.add_textbox(Inches(0.5), Inches(0.22), Inches(9), Inches(0.7))
    tp = title_box.text_frame.paragraphs[0]
    tr = tp.add_run()
    tr.text = "Architecture de l'outil"
    set_run(tr, 26, True, BLANC)

    steps = [
        ("Log Perforce\n(~1,7 Go)", BLEU_FONCE),
        ("LecteurLog\n+ métriques", BLEU),
        ("PerforceEvent\nexport v3", BLEU),
        ("BeepBeep\nPhase 1", ACCENT),
        ("BeepBeep\nPhase 2 CEP", ACCENT),
        ("JSON\n(6 fichiers)", BLEU_FONCE),
    ]

    y = Inches(2.3)
    x_start = Inches(0.35)
    w = Inches(1.22)
    h = Inches(1.1)
    gap = Inches(0.12)

    for i, (label, color) in enumerate(steps):
        x = x_start + i * (w + gap)
        shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        shape.fill.solid()
        shape.fill.fore_color.rgb = color
        shape.line.fill.background()
        tf = shape.text_frame
        tf.word_wrap = True
        tf.vertical_anchor = MSO_ANCHOR.MIDDLE
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = label
        set_run(r, 11, True, BLANC)

        if i < len(steps) - 1:
            arr_x = x + w + Inches(0.02)
            arr = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, arr_x, y + Inches(0.42), Inches(0.14), Inches(0.25))
            arr.fill.solid()
            arr.fill.fore_color.rgb = GRIS
            arr.line.fill.background()

    files_box = slide.shapes.add_textbox(Inches(0.5), Inches(4.0), Inches(8.8), Inches(2.5))
    tf = files_box.text_frame
    tf.word_wrap = True
    items = [
        "8 fichiers Java : Main, LecteurLog, PerforceEvent, MetriquesServeur,",
        "  MetriquesPrometheus, EnrichissementEvenements, UtilEvenements,",
        "  HistogrammeDurees, SummaryDurees",
        "BeepBeep 3.13 (JAR) — pas de compilation depuis les sources",
        "Sorties : perforce_events_v3.json, perforce_gauges.json,",
        "  perforce_histogrammes.json, perforce_summaries.json, perforce_events_alertes.json",
    ]
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(10)
        r = p.add_run()
        r.text = "• " + item
        set_run(r, 16, False, GRIS)


def build():
    prs = Presentation()
    prs.slide_width = Inches(10)
    prs.slide_height = Inches(7.5)

    add_title_slide(
        prs,
        "Détection d'antipatterns\nà partir de logs Perforce",
        "Métriques type Prometheus + patterns temporels CEP avec BeepBeep",
    )

    add_content_slide(
        prs,
        "Contexte et objectif",
        [
            "Développement de jeux vidéo en équipe — cours NAD, dépôt Perforce partagé",
            "Les logs serveur (~1,7 Go) tracent toute l'activité mais sont difficiles à exploiter",
            "Objectif : détecter automatiquement des antipatterns de collaboration",
            "Approche en deux phases (suggestion Sylvain) :",
        ],
        {
            "Approche en deux phases (suggestion Sylvain) :": [
                "Phase 1 — reproduire ce que Prometheus ferait (compteurs, agrégations)",
                "Phase 2 — aller plus loin avec le CEP (séquences temporelles)",
            ],
        },
    )

    add_content_slide(
        prs,
        "Antipatterns ciblés",
        [
            "Procrastination collective — rush de submits avant deadline",
            "Warm Bodies — développeurs peu actifs",
            "Lone Wolf — beaucoup d'edits, peu de submits",
            "Bystander Apathy — checkout (edit) sans submit",
            "Edit → revert — fichier ouvert puis abandonné sans modification",
        ],
    )

    add_table_slide(
        prs,
        "Correspondance catalogue ReliSA",
        ["Notre détection", "Fiche ReliSA", "Note"],
        [
            ["Procrastination collective", "Collective_Procrastination", "Direct"],
            ["Warm Bodies", "Warm_Bodies", "Direct"],
            ["Lone Wolf", "Lone-Wolf", "Direct"],
            ["Edit sans submit", "—", "Proxy log (≠ Bystander_Apathy ReliSA)"],
            ["Edit → revert", "Cascading_Branches", "Approx. — pas de fiche exacte"],
        ],
    )

    add_content_slide(
        prs,
        "Edit → revert et le catalogue ReliSA",
        [
            "Aucune fiche ReliSA ne décrit explicitement edit puis revert Perforce",
            "Le plus proche : Cascading Branches — travail isolé puis abandonné",
            "Bystander_Apathy (ReliSA) = effet témoin social, pas un pattern VCS",
            "Notre edit→revert = opérationnalisation nouvelle à partir des logs",
            "Source : github.com/ReliSA/Software-process-antipatterns-catalogue",
        ],
    )

    add_architecture_slide(prs)

    add_section_slide(prs, "Étape 0 — Extraction des événements")

    add_content_slide(
        prs,
        "Parseur de logs (LecteurLog.java)",
        [
            "Lecture en streaming — fichier ~1,7 Go traité en ~14 s",
            "Ignore le bruit serveur ; conserve les lignes --- après completed",
            "11 commandes user-* (edit, submit, sync, integrate…)",
            "Extraction : timestamp, user, args, fichiers, durée, métriques serveur",
            "Métriques : lapse, memory, rpc, db locks, rows (MetriquesServeur.java)",
            "17 187 événements extraits → 16 262 uniques (dédup BeepBeep)",
        ],
    )

    add_content_slide(
        prs,
        "Export v3 (perforce_events_v3)",
        [
            "EnrichissementEvenements.java — 1 enregistrement par bloc événement, 27 colonnes",
            "file_path : tableau [\"chemin1\",\"chemin2\"] — nb_files",
            "Métriques serveur : duration_s, lapse_s, mem, rpc, db locks, rows",
            "project : NAD/NAND, UE_5.6, dossier Content, segment dépôt",
            "Aligné sur dataset de référence (collaboration équipe)",
            "Fichiers : perforce_events_v3.json (+ .csv optionnel)",
        ],
    )

    add_section_slide(prs, "Phase 1 — Métriques Prometheus")

    add_table_slide(
        prs,
        "Types OpenMetrics — client Java Prometheus",
        ["Type OpenMetrics", "Description", "Notre projet"],
        [
            ["Counter", "Monotone croissant (+1 / événement)", "Oui — Cumulate BeepBeep"],
            ["Gauge", "Monte et descend", "Oui — editsOuverts + export JSON"],
            ["Histogram", "Buckets (durées)", "Oui — duration_s, 15 buckets"],
            ["Summary", "Quantiles p50/p95/p99", "Oui — batch sur log complet"],
            ["Info / StateSet", "Métadonnées, états", "Non"],
            ["Dérivée", "Calcul sur Counters", "Ratio edit/submit (Lone Wolf)"],
            ["CEP (phase 2)", "Séquences temporelles", "Hors OpenMetrics"],
        ],
    )

    add_table_slide(
        prs,
        "Correspondance client Java Prometheus",
        ["Client Java", "Notre projet (BeepBeep)", "Nom métrique"],
        [
            ["counter.inc()", "TurnInto(1) → Cumulate", "perforce_*_total"],
            ["gauge.set() / inc() / dec()", "taille map editsOuverts", "perforce_open_edit_*"],
            ["histogram.observe()", "HistogrammeDurees.observer()", "perforce_command_duration_seconds"],
            ["summary.observe()", "SummaryDurees (quantiles batch)", "perforce_command_duration_seconds"],
            ["labelNames(\"command\")", "champ command / par_commande", "user-edit, user-sync…"],
            ["Unit.SECONDS", "duration_s du log", "unit: seconds"],
        ],
    )

    add_content_slide(
        prs,
        "Counter : BeepBeep ↔ Prometheus Java",
        [
            "OpenMetrics Counter : ne fait qu'augmenter (souvent +1 par événement)",
            "Client Java : counter.inc() à chaque événement",
            "BeepBeep : TurnInto(1) → Cumulate (= inc() répété sur le flux)",
            "Convention : suffixe _total (ex. perforce_user_submit_total)",
            "Export : perforce_counters.json",
            "Phase 2 (edit→revert, apathy) ≠ Counter — c'est du CEP",
        ],
    )

    add_content_slide(
        prs,
        "Gauge : BeepBeep ↔ Prometheus Java",
        [
            "OpenMetrics Gauge : monte et descend (≠ Counter monotone)",
            "Client Java : gauge.inc() / gauge.dec() / gauge.set()",
            "Notre gauge : taille de editsOuverts après chaque edit/submit/revert",
            "Pipeline : QueueSource → FilterOn → état (map) → lecture instantanée",
            "Métriques : perforce_open_edit_files, perforce_open_edit_users",
            "Résultats log réel : fin 250 fichiers, pic 500 (27/04 16h), 79 users max",
            "Export : perforce_gauges.json (résumé + série temporelle)",
        ],
    )

    add_content_slide(
        prs,
        "Histogram : distribution des durées",
        [
            "Métrique : perforce_command_duration_seconds (ligne completed)",
            "BeepBeep : QueueSource → FilterOn(durée) → observe dans buckets",
            "Buckets : 0,05 s → 1200 s (+Inf) — 16 232 observations",
            "user-resolve / submit : quasi tout < 0,25 s",
            "user-sync : distribution étalée (p99 ≈ 18 min) — projets UE",
            "Export : perforce_histogrammes.json",
        ],
    )

    add_content_slide(
        prs,
        "Summary : quantiles des durées",
        [
            "Complète l'Histogram : p50, p90, p95, p99 (quantiles exacts, batch)",
            "Global : p50 = 0,09 s mais p99 = 85 s → outliers user-sync",
            "user-edit : p50 = 0,11 s, p99 = 0,70 s (majorité rapide)",
            "user-sync : p50 = 0,19 s, p95 = 105 s (syncs lourds rares)",
            "La moyenne (7,4 s) est trompeuse — le Summary le montre",
            "Export : perforce_summaries.json",
        ],
    )

    add_content_slide(
        prs,
        "Pipeline BeepBeep — Phase 1 (Counter)",
        [
            "QueueSource → FilterOn → TurnInto(1) → Cumulate",
            "Chaque événement = +1 ; valeur finale = Counter Prometheus",
            "Counters produits :",
        ],
        {
            "Counters produits :": [
                "perforce_events_total, perforce_user_submit_total, perforce_user_edit_total",
                "Submits par heure = Counter + label « heure »",
                "Activité par user = Counter + label « user » (Warm Bodies)",
                "Ratio edit/submit = 2 Counters combinés (Lone Wolf)",
            ],
        },
    )

    add_two_column_slide(
        prs,
        "Résultats — Phase 1 (log réel, juin 2026)",
        "Volumes",
        [
            "17 187 extraits → 16 262 uniques",
            "1 224 user-submit, 4 069 user-edit",
            "Gauge : 250 checkouts ouverts (fin)",
        ],
        "Exemples marquants",
        [
            "Pic 27/04 15h → 107 submits",
            "Gauge pic : 500 fichiers (27/04 16h)",
            "achavy : ratio edit/submit 182",
            "user-sync p99 : 1102 s (sync UE)",
        ],
    )

    add_content_slide(
        prs,
        "Limite de la Phase 1",
        [
            "Les compteurs quantifient l'activité mais ne voient pas l'ordre des actions",
            "Impossible de répondre à :",
        ],
        {
            "Impossible de répondre à :": [
                "« Ce fichier a-t-il été édité puis revert sans modification ? »",
                "« Ce checkout a-t-il jamais été soumis ? »",
                "« Combien de devs distincts ont soumis dans la même heure ? » (au-delà du simple compte)",
            ],
        },
    )

    add_section_slide(prs, "Phase 2 — Patterns temporels CEP")

    add_two_column_slide(
        prs,
        "Counter vs CEP — la différence clé",
        "Counter (Phase 1)",
        [
            "Question : « Combien ? »",
            "Pas de mémoire entre événements",
            "Ex. : 4 069 edits au total",
            "Ex. : 107 submits à 15h le 27/04",
        ],
        "CEP (Phase 2)",
        [
            "Question : « Dans quel ordre ? »",
            "État gardé en mémoire (automate)",
            "Ex. : edit puis revert sur même fichier",
            "Ex. : edit sans submit sur ce fichier",
        ],
    )

    add_content_slide(
        prs,
        "Détecteur d'état — comment ça marche",
        [
            "Map editsOuverts : (user|fichier) → dernier edit",
            "user-edit   → fichier marqué « ouvert »",
            "user-submit → retire les fichiers soumis (ou tous si -i)",
            "user-revert → si edit existait → alerte edit→revert",
            "Fin du log  → fichiers encore ouverts → alerte apathy",
        ],
    )

    add_content_slide(
        prs,
        "Les 3 patterns CEP détectés",
        [
            "Bystander Apathy : edit(fichier) sans submit(fichier) après",
            "Edit → revert : edit(fichier) puis revert(même fichier)",
            "Rush collectif : ≥ N users distincts submit dans la même heure",
            "Correspondance edit ↔ submit/revert :",
        ],
        {
            "Correspondance edit ↔ submit/revert :": [
                "Chemin exact, suffixe (C:/… ↔ //depot/…), nom de fichier",
                "Limite : submits -i sans fichiers listés dans le log",
            ],
        },
    )

    add_table_slide(
        prs,
        "Comparaison Phase 1 vs Phase 2",
        ["Antipattern", "Phase 1 (Prometheus)", "Phase 2 (CEP)"],
        [
            ["Procrastination collective", "Submits / heure", "N users distincts / heure"],
            ["Warm Bodies", "Top / bottom activité", "—"],
            ["Lone Wolf", "Ratio edit/submit", "—"],
            ["Bystander Apathy", "Gauge (fichiers ouverts)", "edit sans submit"],
            ["Edit → revert", "—", "edit puis revert"],
        ],
    )

    add_two_column_slide(
        prs,
        "Résultats — Phase 2 (log réel, juin 2026)",
        "Alertes détectées",
        [
            "217 séquences edit → revert",
            "250 fichiers en apathy (edit sans submit)",
            "Nombreuses fenêtres de rush collectif",
        ],
        "Exemples marquants",
        [
            "27/04 13h : 60 users distincts",
            "27/04 11h : 42 users, 15h : 107 submits",
            "26/04 23h : 31 users (64 submits)",
            "Cohérence gauge fin (250) = apathy (250)",
            "Export : perforce_events_alertes.json",
        ],
    )

    add_content_slide(
        prs,
        "Démonstration / fichiers produits",
        [
            "Entrée : PerforceLog/log.txt (~1,7 Go)",
            "Sorties dans le projet : perforce_events_v3.json/.csv,",
            "  perforce_counters.json, perforce_gauges.json,",
            "  perforce_histogrammes.json, perforce_summaries.json,",
            "  perforce_events_alertes.json",
        ],
    )

    add_content_slide(
        prs,
        "Commande et exécution",
        [
            "Compilation : javac -cp lib/beepbeep-3.13.jar -d bin -sourcepath src src/.../*.java",
            "Exécution :",
        ],
        {
            "Exécution :": [
                "cd PerforceLog && java -cp \"bin:lib/beepbeep-3.13.jar\" ca.uqac.detection.Main",
                "Temps traitement : ~14 s pour 1,7 Go",
            ],
        },
    )

    add_content_slide(
        prs,
        "Piste publication — tool paper",
        [
            "Message central : 4 types OpenMetrics + CEP pour les séquences",
            "Sections : extraction v3, Counter/Gauge/Histogram/Summary, CEP, évaluation",
            "Figures : gauge/heure, histogrammes, quantiles, rush, edit→revert",
            "Évaluation : ~14 s, cohérence gauge ↔ apathy, validation qualitative",
            "Limite : submits -i, quantiles batch vs fenêtre glissante Prometheus",
        ],
    )

    add_content_slide(
        prs,
        "Prochaines étapes",
        [
            "Validation manuelle d'un échantillon d'alertes (vrais / faux positifs)",
            "Graphiques pour le paper (histogrammes, quantiles, gauge)",
            "Discussion anonymisation des usernames pour diffusion",
            "Rédaction du tool paper / intégration au mémoire",
            "Pistes : dashboard Grafana, alertes temps réel, streaming BeepBeep",
        ],
    )

    add_title_slide(
        prs,
        "Questions & discussion",
        "Merci — Hanifah",
    )

    prs.save(OUTPUT)
    print("Présentation créée : " + OUTPUT)


if __name__ == "__main__":
    build()
