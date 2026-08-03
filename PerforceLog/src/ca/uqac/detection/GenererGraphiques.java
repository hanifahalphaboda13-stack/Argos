package ca.uqac.detection;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;

/**
 * Génère les figures PNG (JFreeChart) pour le tool paper à partir des résultats
 * d'analyse PerforceLog.
 */
public final class GenererGraphiques {

    private static final int LARGEUR = 960;
    private static final int HAUTEUR = 540;
    private static final String[] COMMANDES_QUANTILES = {
        "user-edit", "user-submit", "user-add", "user-resolve"
    };

    private GenererGraphiques() {
    }

    public static void generer(String repertoireProjet, List<PerforceEvent> evenements,
            UtilEvenements.ResultatPhase1 phase1, UtilEvenements.ResultatPhase2 phase2)
            throws IOException {
        File dossier = new File(repertoireProjet, "figures");
        if (!dossier.exists() && !dossier.mkdirs()) {
            throw new IOException("Impossible de créer le dossier : " + dossier.getAbsolutePath());
        }

        System.out.println("\n=== Génération des figures (pipeline + JFreeChart) ===");
        System.out.println("Dossier : " + dossier.getAbsolutePath());

        ArchitecturePipeline.generer(new File(dossier, "fig00_architecture_pipeline.png"));
        System.out.println("  → fig00_architecture_pipeline.png");

        graphiqueCompteurs(phase1, new File(dossier, "fig01_compteurs.png"));
        graphiqueSubmitsParHeure(evenements, new File(dossier, "fig02_submits_par_heure.png"));
        graphiqueGaugeOuverts(phase1.getGauges(), new File(dossier, "fig03_gauge_ouverts.png"));
        graphiqueHistogramme(phase1.getHistogrammes().getGlobal(),
            new File(dossier, "fig04_histogram_durees.png"));
        graphiqueQuantiles(phase1.getSummaries(), new File(dossier, "fig05_quantiles_commandes.png"));
        graphiqueQuantilesSync(phase1.getSummaries(), new File(dossier, "fig06_quantiles_sync.png"));
        graphiqueAlertesPhase2(phase2, new File(dossier, "fig07_alertes_phase2.png"));
        graphiqueRushCollectif(phase2, new File(dossier, "fig08_rush_collectif.png"));

        System.out.println("Figures écrites dans figures/ (9 fichiers PNG)");
    }

    private static void graphiqueCompteurs(UtilEvenements.ResultatPhase1 phase1, File fichier)
            throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(phase1.getEventsTotal(), "Valeur", "Événements");
        dataset.addValue(phase1.getSubmitTotal(), "Valeur", "Submits");
        dataset.addValue(phase1.getEditTotal(), "Valeur", "Edits");

        JFreeChart chart = ChartFactory.createBarChart(
            "Compteurs Prometheus (phase 1)",
            null,
            "Nombre",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false);
        styliserBarres(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueSubmitsParHeure(List<PerforceEvent> evenements, File fichier)
            throws IOException {
        Map<String, Integer> parHeure = UtilEvenements.compterSubmitsParHeure(evenements);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> entree : parHeure.entrySet()) {
            dataset.addValue(entree.getValue(), "Submits", formaterHeure(entree.getKey()));
        }

        JFreeChart chart = ChartFactory.createLineChart(
            "Collective Procrastination — submits par heure",
            "Heure",
            "Nombre de submits",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false);
        styliserLignes(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueGaugeOuverts(UtilEvenements.ResultatGauges gauges, File fichier)
            throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Integer> fichiers = gauges.getFichiersOuvertsMaxParHeure();
        Map<String, Integer> users = gauges.getUtilisateursOuvertsMaxParHeure();
        for (String heure : fichiers.keySet()) {
            dataset.addValue(fichiers.get(heure), "Fichiers ouverts", formaterHeure(heure));
        }
        for (String heure : users.keySet()) {
            dataset.addValue(users.get(heure), "Utilisateurs", formaterHeure(heure));
        }

        JFreeChart chart = ChartFactory.createLineChart(
            "Gauge — checkouts actifs (pic par heure)",
            "Heure",
            "Valeur max",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false);
        styliserLignes(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueHistogramme(HistogrammeDurees histo, File fichier)
            throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<HistogrammeDurees.Bucket> buckets = histo.getBuckets();
        int precedent = 0;
        for (HistogrammeDurees.Bucket bucket : buckets) {
            String le = bucket.getLe();
            if ("+Inf".equals(le)) {
                continue;
            }
            int delta = bucket.getCount() - precedent;
            precedent = bucket.getCount();
            String etiquette = "≤ " + le + " s";
            dataset.addValue(delta, "Observations", etiquette);
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Histogram — durée des commandes (global)",
            "Bucket (secondes)",
            "Nombre d'événements",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false);
        styliserBarres(chart);
        inclinerEtiquettes(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueQuantiles(UtilEvenements.ResultatSummaries summaries, File fichier)
            throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, SummaryDurees> parCommande = summaries.getParCommande();
        for (String commande : COMMANDES_QUANTILES) {
            SummaryDurees summary = parCommande.get(commande);
            if (summary == null) {
                continue;
            }
            for (SummaryDurees.Quantile q : summary.getQuantiles()) {
                if (q.getQuantile() == 0.5 || q.getQuantile() == 0.95 || q.getQuantile() == 0.99) {
                    dataset.addValue(q.getValeur(), q.getEtiquette(), etiquetteCommande(commande));
                }
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Summary — quantiles de durée par commande",
            "Commande",
            "Durée (s)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false);
        styliserBarres(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueQuantilesSync(UtilEvenements.ResultatSummaries summaries,
            File fichier) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        SummaryDurees sync = summaries.getParCommande().get("user-sync");
        if (sync != null) {
            for (SummaryDurees.Quantile q : sync.getQuantiles()) {
                dataset.addValue(q.getValeur(), q.getEtiquette(), "user-sync");
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Summary — quantiles user-sync (syncs UE lourdes)",
            null,
            "Durée (s)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false);
        styliserBarres(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueAlertesPhase2(UtilEvenements.ResultatPhase2 phase2, File fichier)
            throws IOException {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Edit → revert", phase2.getEditsReverts().size());
        dataset.setValue("Bystander apathy", phase2.getApathies().size());
        dataset.setValue("Rush collectif (fenêtres)", phase2.getRushs().size());

        JFreeChart chart = ChartFactory.createPieChart(
            "Alertes CEP (phase 2)",
            dataset,
            true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static void graphiqueRushCollectif(UtilEvenements.ResultatPhase2 phase2, File fichier)
            throws IOException {
        List<UtilEvenements.FenetreRush> rushs =
            new ArrayList<UtilEvenements.FenetreRush>(phase2.getRushs());
        Collections.sort(rushs, new Comparator<UtilEvenements.FenetreRush>() {
            @Override
            public int compare(UtilEvenements.FenetreRush a, UtilEvenements.FenetreRush b) {
                return Integer.compare(b.getUtilisateurs().size(), a.getUtilisateurs().size());
            }
        });

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int limite = Math.min(12, rushs.size());
        for (int i = 0; i < limite; i++) {
            UtilEvenements.FenetreRush rush = rushs.get(i);
            dataset.addValue(
                rush.getUtilisateurs().size(),
                "Utilisateurs distincts",
                formaterHeure(rush.getHeure()));
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Rush collectif — utilisateurs distincts / heure (top 12)",
            "Heure",
            "Utilisateurs",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false);
        styliserBarres(chart);
        inclinerEtiquettes(chart);
        sauvegarder(chart, fichier);
        System.out.println("  → " + fichier.getName());
    }

    private static String formaterHeure(String heure) {
        if (heure == null) {
            return "";
        }
        return heure.replace("2026/04/", "04-").replace(" ", " h");
    }

    private static String etiquetteCommande(String commande) {
        if (commande == null) {
            return "";
        }
        return commande.replace("user-", "");
    }

    private static void styliserBarres(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(46, 95, 158));
    }

    private static void styliserLignes(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        inclinerEtiquettes(chart);
    }

    private static void inclinerEtiquettes(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        plot.getDomainAxis().setLowerMargin(0.02);
        plot.getDomainAxis().setUpperMargin(0.02);
    }

    private static void sauvegarder(JFreeChart chart, File fichier) throws IOException {
        chart.setPadding(new RectangleInsets(8, 12, 8, 12));
        ChartUtilities.saveChartAsPNG(fichier, chart, LARGEUR, HAUTEUR);
    }
}
