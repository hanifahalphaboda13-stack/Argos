package ca.uqac.detection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.Pullable;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.CumulativeFunction;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.QueueSource;
import ca.uqac.lif.cep.util.Numbers;

public final class UtilEvenements {

    public enum FormatSortie {
        JSON,
        CSV;

        public static FormatSortie depuis(String valeur) {
            if (valeur == null || valeur.trim().isEmpty()) {
                throw new IllegalArgumentException("Format requis : json ou csv");
            }
            String normalise = valeur.trim().toLowerCase();
            if ("json".equals(normalise)) {
                return JSON;
            }
            if ("csv".equals(normalise)) {
                return CSV;
            }
            throw new IllegalArgumentException("Format inconnu : " + valeur + " (json ou csv)");
        }

        public static FormatSortie depuisChemin(String chemin) {
            if (chemin == null) {
                throw new IllegalArgumentException("Chemin de sortie requis");
            }
            String lower = chemin.toLowerCase();
            if (lower.endsWith(".json")) {
                return JSON;
            }
            if (lower.endsWith(".csv")) {
                return CSV;
            }
            throw new IllegalArgumentException(
                "Extension non reconnue pour " + chemin + " (utilisez .json ou .csv, ou précisez le format)"
            );
        }
    }

    private UtilEvenements() {
    }

    public static List<PerforceEvent> supprimerDoublons(List<PerforceEvent> evenements) {
        Set<String> vus = new LinkedHashSet<String>();
        List<PerforceEvent> uniques = new ArrayList<PerforceEvent>();
        for (PerforceEvent evt : evenements) {
            if (vus.add(evt.cleDedup())) {
                uniques.add(evt);
            }
        }
        return uniques;
    }

    public static void afficherStatistiquesDoublons(List<PerforceEvent> evenements) {
        List<PerforceEvent> sansDoublons = supprimerDoublons(evenements);
        System.out.println("Avec doublons        : " + evenements.size());
        System.out.println("Après suppression    : " + sansDoublons.size());
        System.out.println("Doublons supprimés   : " + (evenements.size() - sansDoublons.size()));
    }

    private static void ecrireChampJSON(BufferedWriter bw, String indent,
                                        String nom, String valeur, boolean dernier)
            throws IOException {
        bw.write(indent + "\"" + nom + "\": \"" + echapperJSON(valeur) + "\"");
        bw.write(dernier ? "\n" : ",\n");
    }

    private static void ecrireListeJSON(BufferedWriter bw, String indent,
                                        String nom, List<String> valeurs, boolean dernierChampObjet)
            throws IOException {
        bw.write(indent + "\"" + nom + "\": [");
        for (int i = 0; i < valeurs.size(); i++) {
            bw.write("\"" + echapperJSON(valeurs.get(i)) + "\"");
            if (i < valeurs.size() - 1) {
                bw.write(", ");
            }
        }
        bw.write("]");
        bw.write(dernierChampObjet ? "\n" : ",\n");
    }

    private static String echapperJSON(String valeur) {
        StringBuilder sb = new StringBuilder();
        for (char c : valeur.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Analyse BeepBeep — métriques de type Prometheus (Counter)
    // -------------------------------------------------------------------------

    public static final class ResultatPhase1 {
        private final long eventsTotal;
        private final long submitTotal;
        private final long editTotal;
        private final ResultatGauges gauges;
        private final ResultatHistogrammes histogrammes;
        private final ResultatSummaries summaries;

        ResultatPhase1(long eventsTotal, long submitTotal, long editTotal,
                       ResultatGauges gauges, ResultatHistogrammes histogrammes,
                       ResultatSummaries summaries) {
            this.eventsTotal = eventsTotal;
            this.submitTotal = submitTotal;
            this.editTotal = editTotal;
            this.gauges = gauges;
            this.histogrammes = histogrammes;
            this.summaries = summaries;
        }

        public long getEventsTotal() { return eventsTotal; }
        public long getSubmitTotal() { return submitTotal; }
        public long getEditTotal() { return editTotal; }
        public ResultatGauges getGauges() { return gauges; }
        public ResultatHistogrammes getHistogrammes() { return histogrammes; }
        public ResultatSummaries getSummaries() { return summaries; }
    }

    public static ResultatPhase1 analyserAvecBeepBeep(List<PerforceEvent> evenements) {
        System.out.println("\n=== Analyse BeepBeep (métriques Prometheus) ===");

        long total = compterAvecBeepBeep(evenements, null);
        long submits = compterAvecBeepBeep(evenements, "user-submit");
        long edits = compterAvecBeepBeep(evenements, "user-edit");

        System.out.println("Counter [" + MetriquesPrometheus.EVENTS_TOTAL + "] : " + total);
        System.out.println("Counter [" + MetriquesPrometheus.USER_SUBMIT_TOTAL + "] : " + submits);
        System.out.println("Counter [" + MetriquesPrometheus.USER_EDIT_TOTAL + "] : " + edits);

        System.out.println("\n--- Collective Procrastination : submits par heure ---");
        Map<String, Integer> submitsParHeure = compterSubmitsParHeure(evenements);
        for (Map.Entry<String, Integer> entree : submitsParHeure.entrySet()) {
            System.out.println("  " + entree.getKey() + " → " + entree.getValue() + " submit(s)");
        }

        System.out.println("\n--- Warm Bodies : activité par utilisateur (top 5 / bottom 5) ---");
        Map<String, Integer> parUser = compterParUtilisateur(evenements);
        afficherTopEtBas(parUser, 5);

        System.out.println("\n--- Lone Wolf : ratio edit/submit par utilisateur ---");
        afficherRatioEditSubmit(evenements);

        ResultatGauges gauges = analyserGaugesAvecBeepBeep(evenements);
        ResultatHistogrammes histogrammes = analyserHistogrammesAvecBeepBeep(evenements);
        ResultatSummaries summaries = analyserSummariesAvecBeepBeep(evenements);
        return new ResultatPhase1(total, submits, edits, gauges, histogrammes, summaries);
    }

    public static void exporterCompteurs(ResultatPhase1 resultat, String cheminJSON) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminJSON), 1 << 20);
        try {
            bw.write("{\n");
            bw.write("  \"type\": \"Counter\",\n");
            bw.write("  \"openmetrics_reference\": \"prometheus/client_java Metric Types\",\n");
            bw.write("  \"pipeline_beepbeep\": \"QueueSource → FilterOn → TurnInto(1) → Cumulate\",\n");
            bw.write("  \"" + MetriquesPrometheus.EVENTS_TOTAL + "\": {\n");
            ecrireChampJSON(bw, "    ", "help", MetriquesPrometheus.HELP_EVENTS_TOTAL, false);
            bw.write("    \"value\": " + resultat.getEventsTotal() + "\n");
            bw.write("  },\n");
            bw.write("  \"" + MetriquesPrometheus.USER_SUBMIT_TOTAL + "\": {\n");
            ecrireChampJSON(bw, "    ", "help", MetriquesPrometheus.HELP_USER_SUBMIT_TOTAL, false);
            bw.write("    \"value\": " + resultat.getSubmitTotal() + "\n");
            bw.write("  },\n");
            bw.write("  \"" + MetriquesPrometheus.USER_EDIT_TOTAL + "\": {\n");
            ecrireChampJSON(bw, "    ", "help", MetriquesPrometheus.HELP_USER_EDIT_TOTAL, false);
            bw.write("    \"value\": " + resultat.getEditTotal() + "\n");
            bw.write("  }\n");
            bw.write("}\n");
        } finally {
            bw.close();
        }
    }

    /**
     * Gauge Prometheus : valeur instantanée qui monte et descend (≠ Counter monotone).
     * BeepBeep : QueueSource → FilterOn(edit/submit/revert) → état editsOuverts ;
     * la taille de la map = valeur du gauge à chaque événement.
     * Équivalent Java : gauge.set(nb) / gauge.inc() / gauge.dec() selon edit, submit, revert.
     */
    public static ResultatGauges analyserGaugesAvecBeepBeep(List<PerforceEvent> evenements) {
        System.out.println("\n--- Gauge : fichiers en edit ouverts (checkout actifs) ---");

        DetecteurPatterns detecteur = new DetecteurPatterns(Integer.MAX_VALUE);
        parcourirFluxPhase2(evenements, detecteur);
        ResultatGauges resultat = detecteur.construireResultatGauges();

        System.out.println("Gauge [" + MetriquesPrometheus.OPEN_EDIT_FILES + "] valeur finale : "
            + resultat.getFichiersOuvertsFin());
        System.out.println("Gauge [" + MetriquesPrometheus.OPEN_EDIT_FILES + "] pic (max)     : "
            + resultat.getFichiersOuvertsMax()
            + " (vers " + resultat.getHeurePicFichiers() + ")");
        System.out.println("Gauge [" + MetriquesPrometheus.OPEN_EDIT_USERS + "] fin  : "
            + resultat.getUtilisateursOuvertsFin());
        System.out.println("Gauge [" + MetriquesPrometheus.OPEN_EDIT_USERS + "] max  : "
            + resultat.getUtilisateursOuvertsMax());

        afficherCourbeGaugeParHeure(resultat);

        return resultat;
    }

    private static void afficherCourbeGaugeParHeure(ResultatGauges resultat) {
        Map<String, Integer> fichiersParHeure = resultat.getFichiersOuvertsMaxParHeure();
        if (fichiersParHeure.isEmpty()) {
            return;
        }

        final int largeurBarre = 40;
        int maxGlobal = resultat.getFichiersOuvertsMax();
        String heurePic = resultat.getHeurePicFichiers();

        System.out.println("\n--- Gauge fichiers ouverts : pic par heure ---");
        for (Map.Entry<String, Integer> entree : fichiersParHeure.entrySet()) {
            String heure = entree.getKey();
            int valeur = entree.getValue();
            String marqueur = heure.equals(heurePic) ? " ◄ pic global" : "";
            System.out.println(String.format("  %s │ %4d %s%s",
                heure, valeur, barreAscii(valeur, maxGlobal, largeurBarre), marqueur));
        }

        Map<String, Integer> usersParHeure = resultat.getUtilisateursOuvertsMaxParHeure();
        int maxUsers = resultat.getUtilisateursOuvertsMax();
        System.out.println("\n--- Gauge utilisateurs avec edits ouverts : pic par heure ---");
        for (Map.Entry<String, Integer> entree : usersParHeure.entrySet()) {
            System.out.println(String.format("  %s │ %4d %s",
                entree.getKey(), entree.getValue(),
                barreAscii(entree.getValue(), maxUsers, largeurBarre)));
        }

        afficherFenetreAutourPic(fichiersParHeure, heurePic, maxGlobal, largeurBarre, 4);
    }

    private static void afficherFenetreAutourPic(Map<String, Integer> parHeure, String heurePic,
            int maxGlobal, int largeurBarre, int rayonHeures) {
        if (heurePic == null || "(aucun)".equals(heurePic) || !parHeure.containsKey(heurePic)) {
            return;
        }
        List<String> heures = new ArrayList<String>(parHeure.keySet());
        int indexPic = heures.indexOf(heurePic);
        int debut = Math.max(0, indexPic - rayonHeures);
        int fin = Math.min(heures.size() - 1, indexPic + rayonHeures);

        System.out.println("\n--- Fenêtre autour du pic (" + heurePic + ") ---");
        for (int i = debut; i <= fin; i++) {
            String heure = heures.get(i);
            int valeur = parHeure.get(heure);
            String marqueur = heure.equals(heurePic) ? " ◄" : "";
            System.out.println(String.format("  %s │ %4d %s%s",
                heure, valeur, barreAscii(valeur, maxGlobal, largeurBarre), marqueur));
        }
    }

    private static String barreAscii(int valeur, int max, int largeur) {
        if (max <= 0 || valeur <= 0) {
            return "";
        }
        int len = (int) ((long) valeur * largeur / max);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append('#');
        }
        return sb.toString();
    }

    public static final class PointGauge {
        private final String timestamp;
        private final String command;
        private final int fichiersOuverts;
        private final int utilisateursOuverts;

        PointGauge(String timestamp, String command, int fichiersOuverts, int utilisateursOuverts) {
            this.timestamp = timestamp;
            this.command = command;
            this.fichiersOuverts = fichiersOuverts;
            this.utilisateursOuverts = utilisateursOuverts;
        }

        public String getTimestamp() { return timestamp; }
        public String getCommand() { return command; }
        public int getFichiersOuverts() { return fichiersOuverts; }
        public int getUtilisateursOuverts() { return utilisateursOuverts; }
    }

    public static final class ResultatGauges {
        private final int fichiersOuvertsFin;
        private final int fichiersOuvertsMax;
        private final String heurePicFichiers;
        private final int utilisateursOuvertsFin;
        private final int utilisateursOuvertsMax;
        private final Map<String, Integer> fichiersOuvertsMaxParHeure;
        private final Map<String, Integer> utilisateursOuvertsMaxParHeure;
        private final List<PointGauge> serie;

        ResultatGauges(int fichiersOuvertsFin, int fichiersOuvertsMax, String heurePicFichiers,
                         int utilisateursOuvertsFin, int utilisateursOuvertsMax,
                         Map<String, Integer> fichiersOuvertsMaxParHeure,
                         Map<String, Integer> utilisateursOuvertsMaxParHeure,
                         List<PointGauge> serie) {
            this.fichiersOuvertsFin = fichiersOuvertsFin;
            this.fichiersOuvertsMax = fichiersOuvertsMax;
            this.heurePicFichiers = heurePicFichiers;
            this.utilisateursOuvertsFin = utilisateursOuvertsFin;
            this.utilisateursOuvertsMax = utilisateursOuvertsMax;
            this.fichiersOuvertsMaxParHeure = Collections.unmodifiableMap(
                new LinkedHashMap<String, Integer>(fichiersOuvertsMaxParHeure));
            this.utilisateursOuvertsMaxParHeure = Collections.unmodifiableMap(
                new LinkedHashMap<String, Integer>(utilisateursOuvertsMaxParHeure));
            this.serie = Collections.unmodifiableList(new ArrayList<PointGauge>(serie));
        }

        public int getFichiersOuvertsFin() { return fichiersOuvertsFin; }
        public int getFichiersOuvertsMax() { return fichiersOuvertsMax; }
        public String getHeurePicFichiers() { return heurePicFichiers; }
        public int getUtilisateursOuvertsFin() { return utilisateursOuvertsFin; }
        public int getUtilisateursOuvertsMax() { return utilisateursOuvertsMax; }
        public Map<String, Integer> getFichiersOuvertsMaxParHeure() {
            return fichiersOuvertsMaxParHeure;
        }
        public Map<String, Integer> getUtilisateursOuvertsMaxParHeure() {
            return utilisateursOuvertsMaxParHeure;
        }
        public List<PointGauge> getSerie() { return serie; }
    }

    public static void exporterGauges(ResultatGauges resultat, String cheminJSON) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminJSON), 1 << 20);
        try {
            bw.write("{\n");
            bw.write("  \"type\": \"Gauge\",\n");
            bw.write("  \"openmetrics_reference\": \"prometheus/client_java Metric Types\",\n");
            bw.write("  \"description\": \"Checkouts Perforce actifs (edit sans submit/revert)\",\n");
            bw.write("  \"pipeline_beepbeep\": \"QueueSource → FilterOn(edit|submit|revert) → état editsOuverts\",\n");

            bw.write("  \"" + MetriquesPrometheus.OPEN_EDIT_FILES + "\": {\n");
            ecrireChampJSON(bw, "    ", "help", MetriquesPrometheus.HELP_OPEN_EDIT_FILES, false);
            bw.write("    \"fin\": " + resultat.getFichiersOuvertsFin() + ",\n");
            bw.write("    \"max\": " + resultat.getFichiersOuvertsMax() + ",\n");
            ecrireChampJSON(bw, "    ", "heure_pic", resultat.getHeurePicFichiers(), true);
            bw.write("  },\n");

            bw.write("  \"" + MetriquesPrometheus.OPEN_EDIT_USERS + "\": {\n");
            ecrireChampJSON(bw, "    ", "help", MetriquesPrometheus.HELP_OPEN_EDIT_USERS, false);
            bw.write("    \"fin\": " + resultat.getUtilisateursOuvertsFin() + ",\n");
            bw.write("    \"max\": " + resultat.getUtilisateursOuvertsMax() + "\n");
            bw.write("  },\n");

            bw.write("  \"pic_par_heure\": {\n");
            bw.write("    \"fichiers\": [\n");
            ecrireValeursParHeureJSON(bw, resultat.getFichiersOuvertsMaxParHeure());
            bw.write("    ],\n");
            bw.write("    \"utilisateurs\": [\n");
            ecrireValeursParHeureJSON(bw, resultat.getUtilisateursOuvertsMaxParHeure());
            bw.write("    ]\n");
            bw.write("  },\n");

            bw.write("  \"serie\": [\n");
            List<PointGauge> serie = resultat.getSerie();
            for (int i = 0; i < serie.size(); i++) {
                PointGauge p = serie.get(i);
                bw.write("    {\n");
                ecrireChampJSON(bw, "      ", "timestamp", p.getTimestamp(), false);
                ecrireChampJSON(bw, "      ", "command", p.getCommand(), false);
                bw.write("      \"fichiers_ouverts\": " + p.getFichiersOuverts() + ",\n");
                bw.write("      \"utilisateurs_ouverts\": " + p.getUtilisateursOuverts() + "\n");
                bw.write("    }");
                bw.write(i < serie.size() - 1 ? ",\n" : "\n");
            }
            bw.write("  ]\n");
            bw.write("}\n");
        } finally {
            bw.close();
        }
    }

    private static void ecrireValeursParHeureJSON(BufferedWriter bw, Map<String, Integer> parHeure)
            throws IOException {
        int i = 0;
        for (Map.Entry<String, Integer> entree : parHeure.entrySet()) {
            bw.write("      {\n");
            ecrireChampJSON(bw, "        ", "heure", entree.getKey(), false);
            bw.write("        \"valeur\": " + entree.getValue() + "\n");
            bw.write("      }");
            i++;
            bw.write(i < parHeure.size() ? ",\n" : "\n");
        }
    }

    /**
     * Histogram Prometheus : distribution de duration_s en buckets cumulatifs.
     * BeepBeep : QueueSource → FilterOn(événements avec durée) → observer(valeur).
     * Équivalent Java : histogram.observe(durationSeconds).
     */
    public static ResultatHistogrammes analyserHistogrammesAvecBeepBeep(
            List<PerforceEvent> evenements) {
        System.out.println("\n--- Histogram : " + MetriquesPrometheus.COMMAND_DURATION_SECONDS + " ---");

        HistogrammeDurees global = new HistogrammeDurees();
        Map<String, HistogrammeDurees> parCommande = new LinkedHashMap<String, HistogrammeDurees>();

        QueueSource source = new QueueSource();
        source.setEvents(evenements.toArray(new Object[0]));
        source.loop(false);
        FilterOn filtre = new FilterOn(new FiltreAvecDuree());
        Connector.connect(source, filtre);

        Pullable pull = filtre.getPullableOutput();
        while (pull.hasNext()) {
            PerforceEvent evt = (PerforceEvent) pull.pull();
            double duree = EnrichissementEvenements.parserDuree(evt.getDuree_s()).doubleValue();
            global.observer(duree);
            String cmd = evt.getCommande();
            HistogrammeDurees histo = parCommande.get(cmd);
            if (histo == null) {
                histo = new HistogrammeDurees();
                parCommande.put(cmd, histo);
            }
            histo.observer(duree);
        }

        ResultatHistogrammes resultat = new ResultatHistogrammes(global, parCommande);

        System.out.println("Histogram [global] count=" + global.getCount()
            + "  sum=" + formaterDuree(global.getSum())
            + "s  avg=" + formaterDuree(global.getMoyenne()) + "s");
        afficherHistogrammeConsole(global);

        String[] commandesCles = {"user-edit", "user-submit", "user-sync", "user-resolve"};
        for (String cmd : commandesCles) {
            HistogrammeDurees h = parCommande.get(cmd);
            if (h != null && h.getCount() > 0) {
                System.out.println("\nHistogram [" + cmd + "] count=" + h.getCount()
                    + "  sum=" + formaterDuree(h.getSum())
                    + "s  avg=" + formaterDuree(h.getMoyenne()) + "s");
                afficherHistogrammeConsole(h);
            }
        }

        return resultat;
    }

    private static void afficherHistogrammeConsole(HistogrammeDurees histo) {
        if (histo.getCount() == 0) {
            return;
        }
        int[] parBucket = histo.comptagesParBucket();
        double[] bornes = histo.getBornes();
        int max = 0;
        for (int n : parBucket) {
            if (n > max) {
                max = n;
            }
        }
        final int largeurBarre = 30;
        for (int i = 0; i < bornes.length; i++) {
            if (parBucket[i] == 0) {
                continue;
            }
            String borne;
            if (Double.isInfinite(bornes[i])) {
                borne = "+Inf";
            } else if (bornes[i] < 1) {
                borne = String.format("≤%.2fs", bornes[i]);
            } else {
                borne = String.format("≤%.0fs", bornes[i]);
            }
            System.out.println(String.format("  %8s │ %5d %s",
                borne, parBucket[i], barreAscii(parBucket[i], max, largeurBarre)));
        }
    }

    private static String formaterDuree(double secondes) {
        return String.format("%.3f", secondes);
    }

    public static final class ResultatHistogrammes {
        private final HistogrammeDurees global;
        private final Map<String, HistogrammeDurees> parCommande;

        ResultatHistogrammes(HistogrammeDurees global, Map<String, HistogrammeDurees> parCommande) {
            this.global = global;
            this.parCommande = Collections.unmodifiableMap(
                new LinkedHashMap<String, HistogrammeDurees>(parCommande));
        }

        public HistogrammeDurees getGlobal() { return global; }
        public Map<String, HistogrammeDurees> getParCommande() { return parCommande; }
    }

    public static void exporterHistogrammes(ResultatHistogrammes resultat, String cheminJSON)
            throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminJSON), 1 << 20);
        try {
            bw.write("{\n");
            bw.write("  \"type\": \"Histogram\",\n");
            bw.write("  \"openmetrics_reference\": \"prometheus/client_java Metric Types\",\n");
            bw.write("  \"metric\": \"" + MetriquesPrometheus.COMMAND_DURATION_SECONDS + "\",\n");
            ecrireChampJSON(bw, "  ", "help", MetriquesPrometheus.HELP_COMMAND_DURATION, false);
            ecrireChampJSON(bw, "  ", "unit", MetriquesPrometheus.UNIT_SECONDS, false);
            bw.write("  \"description\": \"Distribution des durées (ligne completed)\",\n");
            bw.write("  \"pipeline_beepbeep\": \"QueueSource → FilterOn(durée présente) → observe(duration_s)\",\n");
            bw.write("  \"buckets_le_seconds\": [");
            ecrireBornesJSON(bw, HistogrammeDurees.BORNES_DEFAUT);
            bw.write("],\n");
            bw.write("  \"global\": ");
            ecrireHistogrammeJSON(bw, "  ", resultat.getGlobal());
            bw.write(",\n  \"par_commande\": {\n");
            int i = 0;
            int nb = resultat.getParCommande().size();
            for (Map.Entry<String, HistogrammeDurees> entree : resultat.getParCommande().entrySet()) {
                bw.write("    \"" + echapperCleJSON(entree.getKey()) + "\": ");
                ecrireHistogrammeAvecLabelJSON(bw, "    ", entree.getKey(), entree.getValue());
                i++;
                bw.write(i < nb ? ",\n" : "\n");
            }
            bw.write("  }\n");
            bw.write("}\n");
        } finally {
            bw.close();
        }
    }

    private static void ecrireBornesJSON(BufferedWriter bw, double[] bornes) throws IOException {
        for (int i = 0; i < bornes.length; i++) {
            if (i > 0) {
                bw.write(", ");
            }
            if (Double.isInfinite(bornes[i])) {
                bw.write("\"+Inf\"");
            } else {
                bw.write(String.valueOf(bornes[i]));
            }
        }
    }

    private static void ecrireHistogrammeAvecLabelJSON(BufferedWriter bw, String indent,
            String commande, HistogrammeDurees histo) throws IOException {
        bw.write("{\n");
        ecrireChampJSON(bw, indent + "  ", "command", commande, false);
        ecrireHistogrammeJSON(bw, indent, histo, true);
        bw.write("\n" + indent + "}");
    }

    private static void ecrireHistogrammeJSON(BufferedWriter bw, String indent,
            HistogrammeDurees histo) throws IOException {
        ecrireHistogrammeJSON(bw, indent, histo, false);
    }

    private static void ecrireHistogrammeJSON(BufferedWriter bw, String indent,
            HistogrammeDurees histo, boolean imbrique) throws IOException {
        if (!imbrique) {
            bw.write("{\n");
        }
        bw.write(indent + "  \"count\": " + histo.getCount() + ",\n");
        bw.write(indent + "  \"sum\": " + histo.getSum() + ",\n");
        bw.write(indent + "  \"avg\": " + histo.getMoyenne() + ",\n");
        bw.write(indent + "  \"buckets\": [\n");
        List<HistogrammeDurees.Bucket> buckets = histo.getBuckets();
        for (int i = 0; i < buckets.size(); i++) {
            HistogrammeDurees.Bucket b = buckets.get(i);
            bw.write(indent + "    { ");
            bw.write("\"le\": \"" + b.getLe() + "\", ");
            bw.write("\"count\": " + b.getCount());
            bw.write(" }");
            bw.write(i < buckets.size() - 1 ? ",\n" : "\n");
        }
        bw.write(indent + "  ]\n");
        bw.write(indent + "}");
    }

    private static String echapperCleJSON(String cle) {
        return cle.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Summary Prometheus : quantiles (p50, p90, p95, p99) sur duration_s.
     * BeepBeep : QueueSource → FilterOn(durée présente) → observe + tri batch.
     * Équivalent Java : summary.observe(durationSeconds) ; quantiles sur log complet.
     */
    public static ResultatSummaries analyserSummariesAvecBeepBeep(List<PerforceEvent> evenements) {
        System.out.println("\n--- Summary : " + MetriquesPrometheus.COMMAND_DURATION_SECONDS + " ---");

        SummaryDurees global = new SummaryDurees();
        Map<String, SummaryDurees> parCommande = new LinkedHashMap<String, SummaryDurees>();

        QueueSource source = new QueueSource();
        source.setEvents(evenements.toArray(new Object[0]));
        source.loop(false);
        FilterOn filtre = new FilterOn(new FiltreAvecDuree());
        Connector.connect(source, filtre);

        Pullable pull = filtre.getPullableOutput();
        while (pull.hasNext()) {
            PerforceEvent evt = (PerforceEvent) pull.pull();
            double duree = EnrichissementEvenements.parserDuree(evt.getDuree_s()).doubleValue();
            global.observer(duree);
            String cmd = evt.getCommande();
            SummaryDurees summary = parCommande.get(cmd);
            if (summary == null) {
                summary = new SummaryDurees();
                parCommande.put(cmd, summary);
            }
            summary.observer(duree);
        }

        ResultatSummaries resultat = new ResultatSummaries(global, parCommande);

        afficherSummaryConsole("global", global);

        String[] commandesCles = {"user-edit", "user-submit", "user-sync", "user-resolve"};
        for (String cmd : commandesCles) {
            SummaryDurees s = parCommande.get(cmd);
            if (s != null && s.getCount() > 0) {
                afficherSummaryConsole(cmd, s);
            }
        }

        return resultat;
    }

    private static void afficherSummaryConsole(String etiquette, SummaryDurees summary) {
        System.out.println("\nSummary [" + etiquette + "] count=" + summary.getCount()
            + "  sum=" + formaterDuree(summary.getSum())
            + "s  avg=" + formaterDuree(summary.getMoyenne()) + "s"
            + "  max=" + formaterDuree(summary.getMax()) + "s");
        for (SummaryDurees.Quantile q : summary.getQuantiles()) {
            System.out.println(String.format("  %4s │ %9s s",
                q.getEtiquette(), formaterDuree(q.getValeur())));
        }
    }

    public static final class ResultatSummaries {
        private final SummaryDurees global;
        private final Map<String, SummaryDurees> parCommande;

        ResultatSummaries(SummaryDurees global, Map<String, SummaryDurees> parCommande) {
            this.global = global;
            this.parCommande = Collections.unmodifiableMap(
                new LinkedHashMap<String, SummaryDurees>(parCommande));
        }

        public SummaryDurees getGlobal() { return global; }
        public Map<String, SummaryDurees> getParCommande() { return parCommande; }
    }

    public static void exporterSummaries(ResultatSummaries resultat, String cheminJSON)
            throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminJSON), 1 << 20);
        try {
            bw.write("{\n");
            bw.write("  \"type\": \"Summary\",\n");
            bw.write("  \"openmetrics_reference\": \"prometheus/client_java Metric Types\",\n");
            bw.write("  \"metric\": \"" + MetriquesPrometheus.COMMAND_DURATION_SECONDS + "\",\n");
            ecrireChampJSON(bw, "  ", "help", MetriquesPrometheus.HELP_COMMAND_DURATION, false);
            ecrireChampJSON(bw, "  ", "unit", MetriquesPrometheus.UNIT_SECONDS, false);
            bw.write("  \"description\": \"Quantiles des durées (ligne completed)\",\n");
            bw.write("  \"pipeline_beepbeep\": \"QueueSource → FilterOn(durée présente) → observe + quantiles batch\",\n");
            bw.write("  \"note\": \"Quantiles exacts sur le log complet (≠ fenêtre glissante Prometheus live)\",\n");
            bw.write("  \"quantiles\": [0.5, 0.9, 0.95, 0.99],\n");
            bw.write("  \"global\": ");
            ecrireSummaryJSON(bw, "  ", resultat.getGlobal());
            bw.write(",\n  \"par_commande\": {\n");
            int i = 0;
            int nb = resultat.getParCommande().size();
            for (Map.Entry<String, SummaryDurees> entree : resultat.getParCommande().entrySet()) {
                bw.write("    \"" + echapperCleJSON(entree.getKey()) + "\": ");
                ecrireSummaryAvecLabelJSON(bw, "    ", entree.getKey(), entree.getValue());
                i++;
                bw.write(i < nb ? ",\n" : "\n");
            }
            bw.write("  }\n");
            bw.write("}\n");
        } finally {
            bw.close();
        }
    }

    private static void ecrireSummaryAvecLabelJSON(BufferedWriter bw, String indent,
            String commande, SummaryDurees summary) throws IOException {
        bw.write("{\n");
        ecrireChampJSON(bw, indent + "  ", "command", commande, false);
        ecrireSummaryJSON(bw, indent, summary, true);
        bw.write("\n" + indent + "}");
    }

    private static void ecrireSummaryJSON(BufferedWriter bw, String indent, SummaryDurees summary)
            throws IOException {
        ecrireSummaryJSON(bw, indent, summary, false);
    }

    private static void ecrireSummaryJSON(BufferedWriter bw, String indent, SummaryDurees summary,
            boolean imbrique) throws IOException {
        if (!imbrique) {
            bw.write("{\n");
        }
        bw.write(indent + "  \"count\": " + summary.getCount() + ",\n");
        bw.write(indent + "  \"sum\": " + summary.getSum() + ",\n");
        bw.write(indent + "  \"avg\": " + summary.getMoyenne() + ",\n");
        bw.write(indent + "  \"max\": " + summary.getMax() + ",\n");
        bw.write(indent + "  \"quantiles\": [\n");
        List<SummaryDurees.Quantile> quantiles = summary.getQuantiles();
        for (int i = 0; i < quantiles.size(); i++) {
            SummaryDurees.Quantile q = quantiles.get(i);
            bw.write(indent + "    { ");
            bw.write("\"quantile\": " + q.getQuantile() + ", ");
            bw.write("\"etiquette\": \"" + q.getEtiquette() + "\", ");
            bw.write("\"valeur\": " + q.getValeur());
            bw.write(" }");
            bw.write(i < quantiles.size() - 1 ? ",\n" : "\n");
        }
        bw.write(indent + "  ]\n");
        bw.write(indent + "}");
    }

    /**
     * Counter BeepBeep : filtre optionnel → transforme chaque événement en 1 → cumule.
     * Équivalent Prometheus Counter.
     */
    public static long compterAvecBeepBeep(List<PerforceEvent> evenements, String commande) {
        QueueSource source = new QueueSource();
        source.setEvents(evenements.toArray(new Object[0]));
        source.loop(false);

        Processor fin;
        if (commande == null) {
            fin = Connector.connect(
                source,
                new TurnInto(1),
                new Cumulate(new CumulativeFunction<Number>(Numbers.addition))
            );
        } else {
            fin = Connector.connect(
                source,
                new FilterOn(new FiltreCommande(commande)),
                new TurnInto(1),
                new Cumulate(new CumulativeFunction<Number>(Numbers.addition))
            );
        }

        Pullable pull = fin.getPullableOutput();
        Number valeur = 0;
        while (pull.hasNext()) {
            valeur = (Number) pull.pull();
        }
        return valeur.longValue();
    }

    /** Filtre BeepBeep puis agrégation des submits par heure. */
    public static Map<String, Integer> compterSubmitsParHeure(List<PerforceEvent> evenements) {
        QueueSource source = new QueueSource();
        source.setEvents(evenements.toArray(new Object[0]));
        source.loop(false);
        FilterOn filtre = new FilterOn(new FiltreCommande("user-submit"));
        Connector.connect(source, filtre);

        Pullable pull = filtre.getPullableOutput();
        Map<String, Integer> parHeure = new LinkedHashMap<String, Integer>();
        while (pull.hasNext()) {
            PerforceEvent evt = (PerforceEvent) pull.pull();
            String heure = extraireHeure(evt.getTimestamp());
            Integer nb = parHeure.get(heure);
            parHeure.put(heure, nb == null ? 1 : nb + 1);
        }
        return parHeure;
    }

    public static Map<String, Integer> compterParUtilisateur(List<PerforceEvent> evenements) {
        Map<String, Integer> compteur = new LinkedHashMap<String, Integer>();
        for (PerforceEvent evt : evenements) {
            String user = evt.getUser();
            Integer nb = compteur.get(user);
            compteur.put(user, nb == null ? 1 : nb + 1);
        }
        return compteur;
    }

    private static void afficherRatioEditSubmit(List<PerforceEvent> evenements) {
        Map<String, Integer> edits = new LinkedHashMap<String, Integer>();
        Map<String, Integer> submits = new LinkedHashMap<String, Integer>();

        for (PerforceEvent evt : evenements) {
            String user = evt.getUser();
            if ("user-edit".equals(evt.getCommande())) {
                edits.put(user, getOuZero(edits, user) + 1);
            } else if ("user-submit".equals(evt.getCommande())) {
                submits.put(user, getOuZero(submits, user) + 1);
            }
        }

        Set<String> users = new LinkedHashSet<String>();
        users.addAll(edits.keySet());
        users.addAll(submits.keySet());

        for (String user : users) {
            int nbEdit = getOuZero(edits, user);
            int nbSubmit = getOuZero(submits, user);
            System.out.println("  " + user + " → edits=" + nbEdit
                + ", submits=" + nbSubmit
                + ", ratio=" + formaterRatio(nbEdit, nbSubmit));
        }
    }

    private static void afficherTopEtBas(Map<String, Integer> parUser, int n) {
        List<Map.Entry<String, Integer>> liste =
            new ArrayList<Map.Entry<String, Integer>>(parUser.entrySet());
        Collections.sort(liste, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        System.out.println("  Top " + n + " (plus actifs) :");
        for (int i = 0; i < n && i < liste.size(); i++) {
            Map.Entry<String, Integer> e = liste.get(i);
            System.out.println("    " + e.getKey() + " → " + e.getValue());
        }

        System.out.println("  Bottom " + n + " (moins actifs) :");
        for (int i = liste.size() - 1; i >= 0 && i >= liste.size() - n; i--) {
            Map.Entry<String, Integer> e = liste.get(i);
            System.out.println("    " + e.getKey() + " → " + e.getValue());
        }
    }

    private static int getOuZero(Map<String, Integer> map, String cle) {
        Integer val = map.get(cle);
        return val == null ? 0 : val;
    }

    private static String formaterRatio(int edits, int submits) {
        if (submits == 0) {
            return edits > 0 ? "infini (edits sans submit)" : "0";
        }
        return String.format("%.2f", (double) edits / submits);
    }

    private static String extraireHeure(String timestamp) {
        if (timestamp == null || timestamp.length() < 13) {
            return timestamp;
        }
        return timestamp.substring(0, 13);
    }

    // -------------------------------------------------------------------------
    // Analyse BeepBeep Phase 2 — patterns temporels CEP
    // -------------------------------------------------------------------------

    public static final int SEUIL_RUSH_DEFAUT = 3;
    private static final int MAX_EXEMPLES_ALERTES = 15;

    public static final class ResultatPhase2 {
        private final int seuilRush;
        private final List<AlerteEditRevert> editsReverts;
        private final List<AlerteApathy> apathies;
        private final List<FenetreRush> rushs;

        ResultatPhase2(int seuilRush, List<AlerteEditRevert> editsReverts,
                       List<AlerteApathy> apathies, List<FenetreRush> rushs) {
            this.seuilRush = seuilRush;
            this.editsReverts = editsReverts;
            this.apathies = apathies;
            this.rushs = rushs;
        }

        public int getSeuilRush() { return seuilRush; }
        public List<AlerteEditRevert> getEditsReverts() { return editsReverts; }
        public List<AlerteApathy> getApathies() { return apathies; }
        public List<FenetreRush> getRushs() { return rushs; }
    }

    public static final class AlerteEditRevert {
        private final String user;
        private final String fichier;
        private final String editTimestamp;
        private final String revertTimestamp;

        AlerteEditRevert(String user, String fichier, String editTimestamp, String revertTimestamp) {
            this.user = user;
            this.fichier = fichier;
            this.editTimestamp = editTimestamp;
            this.revertTimestamp = revertTimestamp;
        }

        public String getUser() { return user; }
        public String getFichier() { return fichier; }
        public String getEditTimestamp() { return editTimestamp; }
        public String getRevertTimestamp() { return revertTimestamp; }
    }

    public static final class AlerteApathy {
        private final String user;
        private final String fichier;
        private final String editTimestamp;

        AlerteApathy(String user, String fichier, String editTimestamp) {
            this.user = user;
            this.fichier = fichier;
            this.editTimestamp = editTimestamp;
        }

        public String getUser() { return user; }
        public String getFichier() { return fichier; }
        public String getEditTimestamp() { return editTimestamp; }
    }

    public static final class FenetreRush {
        private final String heure;
        private final List<String> utilisateurs;

        FenetreRush(String heure, List<String> utilisateurs) {
            this.heure = heure;
            this.utilisateurs = utilisateurs;
        }

        public String getHeure() { return heure; }
        public List<String> getUtilisateurs() { return utilisateurs; }
    }

    public static ResultatPhase2 analyserPhase2(List<PerforceEvent> evenements) {
        return analyserPhase2(evenements, SEUIL_RUSH_DEFAUT);
    }

    public static ResultatPhase2 analyserPhase2(List<PerforceEvent> evenements, int seuilRush) {
        System.out.println("\n=== Analyse BeepBeep Phase 2 (patterns temporels CEP) ===");
        System.out.println("  Seuil rush : ≥ " + seuilRush + " utilisateurs / heure");

        DetecteurPatterns detecteur = new DetecteurPatterns(seuilRush);
        parcourirFluxPhase2(evenements, detecteur);
        ResultatPhase2 resultat = detecteur.construireResultat();
        afficherResultatsPhase2(resultat);
        return resultat;
    }

    public static String cheminAlertesDepuis(String cheminEvenements) {
        String base = cheminEvenements;
        if (base.endsWith("perforce_events_v3.json")) {
            return base.substring(0, base.length() - "perforce_events_v3.json".length())
                + "perforce_events_alertes.json";
        }
        if (base.endsWith("perforce_events_v3.csv")) {
            return base.substring(0, base.length() - "perforce_events_v3.csv".length())
                + "perforce_events_alertes.json";
        }
        if (base.endsWith("_analytique.json")) {
            return base.substring(0, base.length() - "_analytique.json".length()) + "_alertes.json";
        }
        if (base.endsWith("_analytique.csv")) {
            return base.substring(0, base.length() - "_analytique.csv".length()) + "_alertes.json";
        }
        if (base.endsWith(".json")) {
            return base.substring(0, base.length() - 5) + "_alertes.json";
        }
        if (base.endsWith(".csv")) {
            return base.substring(0, base.length() - 4) + "_alertes.json";
        }
        return base + "_alertes.json";
    }

    public static void exporterAlertesPhase2(ResultatPhase2 resultat, String cheminJSON)
            throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminJSON), 1 << 20);
        try {
            bw.write("{\n");
            bw.write("  \"seuil_rush\": " + resultat.getSeuilRush() + ",\n");
            bw.write("  \"resolution_submit\": \"exacte, suffixe de chemin ou nom de fichier\",\n");

            bw.write("  \"edit_revert\": [\n");
            ecrireAlertesEditRevertJSON(bw, resultat.getEditsReverts());
            bw.write("  ],\n");

            bw.write("  \"bystander_apathy\": [\n");
            ecrireAlertesApathyJSON(bw, resultat.getApathies());
            bw.write("  ],\n");

            bw.write("  \"rush_collectif\": [\n");
            ecrireRushsJSON(bw, resultat.getRushs());
            bw.write("  ]\n");
            bw.write("}\n");
        } finally {
            bw.close();
        }
    }

    private static void ecrireAlertesEditRevertJSON(BufferedWriter bw,
                                                    List<AlerteEditRevert> alertes)
            throws IOException {
        for (int i = 0; i < alertes.size(); i++) {
            AlerteEditRevert a = alertes.get(i);
            bw.write("    {\n");
            ecrireChampJSON(bw, "      ", "user", a.getUser(), false);
            ecrireChampJSON(bw, "      ", "fichier", a.getFichier(), false);
            ecrireChampJSON(bw, "      ", "edit_timestamp", a.getEditTimestamp(), false);
            ecrireChampJSON(bw, "      ", "revert_timestamp", a.getRevertTimestamp(), true);
            bw.write("    }");
            bw.write(i < alertes.size() - 1 ? ",\n" : "\n");
        }
    }

    private static void ecrireAlertesApathyJSON(BufferedWriter bw, List<AlerteApathy> alertes)
            throws IOException {
        for (int i = 0; i < alertes.size(); i++) {
            AlerteApathy a = alertes.get(i);
            bw.write("    {\n");
            ecrireChampJSON(bw, "      ", "user", a.getUser(), false);
            ecrireChampJSON(bw, "      ", "fichier", a.getFichier(), false);
            ecrireChampJSON(bw, "      ", "edit_timestamp", a.getEditTimestamp(), true);
            bw.write("    }");
            bw.write(i < alertes.size() - 1 ? ",\n" : "\n");
        }
    }

    private static void ecrireRushsJSON(BufferedWriter bw, List<FenetreRush> rushs)
            throws IOException {
        for (int i = 0; i < rushs.size(); i++) {
            FenetreRush r = rushs.get(i);
            bw.write("    {\n");
            ecrireChampJSON(bw, "      ", "heure", r.getHeure(), false);
            bw.write("      \"nb_utilisateurs\": " + r.getUtilisateurs().size() + ",\n");
            ecrireListeJSON(bw, "      ", "utilisateurs", r.getUtilisateurs(), true);
            bw.write("    }");
            bw.write(i < rushs.size() - 1 ? ",\n" : "\n");
        }
    }

    private static void afficherResultatsPhase2(ResultatPhase2 resultat) {
        afficherEditsReverts(resultat.getEditsReverts());
        afficherApathies(resultat.getApathies());
        afficherRushCollectif(resultat);
    }

    private static void afficherEditsReverts(List<AlerteEditRevert> alertes) {
        System.out.println("\n--- Edit puis revert (sans modification) ---");
        System.out.println("  Alertes détectées : " + alertes.size());
        int limite = Math.min(MAX_EXEMPLES_ALERTES, alertes.size());
        for (int i = 0; i < limite; i++) {
            AlerteEditRevert a = alertes.get(i);
            System.out.println("  " + a.getUser() + " | " + a.getFichier()
                + " | edit " + a.getEditTimestamp() + " → revert " + a.getRevertTimestamp());
        }
        if (alertes.size() > limite) {
            System.out.println("  ... et " + (alertes.size() - limite) + " autre(s)");
        }
    }

    private static void afficherApathies(List<AlerteApathy> alertes) {
        System.out.println("\n--- Bystander Apathy : edit sans submit ---");
        System.out.println("  Fichiers encore ouverts (edit sans submit) : " + alertes.size());
        int limite = Math.min(MAX_EXEMPLES_ALERTES, alertes.size());
        for (int i = 0; i < limite; i++) {
            AlerteApathy a = alertes.get(i);
            System.out.println("  " + a.getUser() + " | " + a.getFichier()
                + " | edit " + a.getEditTimestamp() + " (jamais soumis)");
        }
        if (alertes.size() > limite) {
            System.out.println("  ... et " + (alertes.size() - limite) + " autre(s)");
        }
    }

    private static void afficherRushCollectif(ResultatPhase2 resultat) {
        System.out.println("\n--- Collective Procrastination : rush multi-utilisateurs ---");
        System.out.println("  Seuil : ≥ " + resultat.getSeuilRush()
            + " utilisateurs distincts soumettant dans la même heure");
        if (resultat.getRushs().isEmpty()) {
            System.out.println("  Aucune fenêtre horaire au-dessus du seuil.");
            return;
        }
        int limite = Math.min(MAX_EXEMPLES_ALERTES, resultat.getRushs().size());
        for (int i = 0; i < limite; i++) {
            FenetreRush r = resultat.getRushs().get(i);
            System.out.println("  " + r.getHeure() + " → " + r.getUtilisateurs().size()
                + " utilisateur(s) : " + r.getUtilisateurs());
        }
        if (resultat.getRushs().size() > limite) {
            System.out.println("  ... et " + (resultat.getRushs().size() - limite) + " autre(s)");
        }
    }

    /**
     * Flux BeepBeep : QueueSource → FilterOn(edit/submit/revert) → détecteur d'état.
     * Le détecteur consomme les événements dans l'ordre chronologique du log.
     */
    private static void parcourirFluxPhase2(List<PerforceEvent> evenements,
                                            DetecteurPatterns detecteur) {
        QueueSource source = new QueueSource();
        source.setEvents(evenements.toArray(new Object[0]));
        source.loop(false);

        FilterOn filtre = new FilterOn(new FiltreCommandesPhase2());
        Connector.connect(source, filtre);

        Pullable pull = filtre.getPullableOutput();
        while (pull.hasNext()) {
            detecteur.traiter((PerforceEvent) pull.pull());
        }
        detecteur.finaliser();
    }

    private static String normaliserFichier(String chemin) {
        if (chemin == null) {
            return "";
        }
        String normalise = chemin.trim().toLowerCase().replace('\\', '/');
        int revision = normalise.indexOf('#');
        if (revision >= 0) {
            normalise = normalise.substring(0, revision);
        }
        return normalise;
    }

    private static String cleUserFichier(String user, String fichier) {
        return user + "|" + normaliserFichier(fichier);
    }

    private static String extraireNomFichier(String cheminNormalise) {
        int slash = cheminNormalise.lastIndexOf('/');
        return slash >= 0 ? cheminNormalise.substring(slash + 1) : cheminNormalise;
    }

    private static String extraireFichierDeCle(String cleUserFichier) {
        int sep = cleUserFichier.indexOf('|');
        return sep >= 0 ? cleUserFichier.substring(sep + 1) : cleUserFichier;
    }

    /**
     * Correspondance edit ↔ submit : chemin exact, suffixe commun, ou même nom de fichier.
     * Utile quand l'edit est en chemin local et le submit en chemin dépôt //...
     */
    private static boolean fichiersCorrespondent(String cheminEdit, String cheminSubmit) {
        String edit = normaliserFichier(cheminEdit);
        String submit = normaliserFichier(cheminSubmit);
        if (edit.isEmpty() || submit.isEmpty()) {
            return false;
        }
        if (edit.equals(submit)) {
            return true;
        }
        if (edit.endsWith(submit) || submit.endsWith(edit)) {
            return true;
        }
        String nomEdit = extraireNomFichier(edit);
        String nomSubmit = extraireNomFichier(submit);
        return !nomEdit.isEmpty() && nomEdit.equals(nomSubmit);
    }

    private static final class DetecteurPatterns {
        private final int seuilRush;
        private final Map<String, PerforceEvent> editsOuverts =
            new LinkedHashMap<String, PerforceEvent>();
        private final List<AlerteEditRevert> editsReverts = new ArrayList<AlerteEditRevert>();
        private final List<AlerteApathy> apathies = new ArrayList<AlerteApathy>();
        private final Map<String, Set<String>> submittersParHeure =
            new LinkedHashMap<String, Set<String>>();
        private int gaugeFichiersOuvertsMax = 0;
        private int gaugeUtilisateursOuvertsMax = 0;
        private String heurePicFichiersOuverts = "";
        private final Map<String, Integer> gaugeFichiersMaxParHeure =
            new LinkedHashMap<String, Integer>();
        private final Map<String, Integer> gaugeUtilisateursMaxParHeure =
            new LinkedHashMap<String, Integer>();
        private final List<PointGauge> serieGauge = new ArrayList<PointGauge>();

        DetecteurPatterns(int seuilRush) {
            this.seuilRush = seuilRush;
        }

        void traiter(PerforceEvent evt) {
            String cmd = evt.getCommande();
            if ("user-edit".equals(cmd)) {
                for (String fichier : evt.getFichiers()) {
                    editsOuverts.put(cleUserFichier(evt.getUser(), fichier), evt);
                }
            } else if ("user-submit".equals(cmd)) {
                enregistrerSubmitHeure(evt);
                resoudreEditsApresSubmit(evt);
            } else if ("user-revert".equals(cmd)) {
                for (String fichier : evt.getFichiers()) {
                    String cle = cleUserFichier(evt.getUser(), fichier);
                    PerforceEvent edit = editsOuverts.remove(cle);
                    if (edit != null) {
                        editsReverts.add(new AlerteEditRevert(
                            edit.getUser(), fichier, edit.getTimestamp(), evt.getTimestamp()));
                        continue;
                    }
                    PerforceEvent editFlou = retirerParCorrespondanceFloue(evt.getUser(), fichier);
                    if (editFlou != null) {
                        editsReverts.add(new AlerteEditRevert(
                            editFlou.getUser(), fichier, editFlou.getTimestamp(), evt.getTimestamp()));
                    }
                }
            }
            mettreAJourGauge(evt.getTimestamp(), cmd);
        }

        void finaliser() {
            for (Map.Entry<String, PerforceEvent> entree : editsOuverts.entrySet()) {
                PerforceEvent edit = entree.getValue();
                apathies.add(new AlerteApathy(
                    edit.getUser(),
                    extraireFichierDeCle(entree.getKey()),
                    edit.getTimestamp()));
            }
        }

        ResultatPhase2 construireResultat() {
            List<FenetreRush> rushs = new ArrayList<FenetreRush>();
            for (Map.Entry<String, Set<String>> entree : submittersParHeure.entrySet()) {
                if (entree.getValue().size() >= seuilRush) {
                    rushs.add(new FenetreRush(
                        entree.getKey(),
                        new ArrayList<String>(entree.getValue())));
                }
            }
            return new ResultatPhase2(seuilRush, editsReverts, apathies, rushs);
        }

        ResultatGauges construireResultatGauges() {
            return new ResultatGauges(
                editsOuverts.size(),
                gaugeFichiersOuvertsMax,
                heurePicFichiersOuverts.isEmpty() ? "(aucun)" : heurePicFichiersOuverts,
                compterUtilisateursDansEditsOuverts(),
                gaugeUtilisateursOuvertsMax,
                gaugeFichiersMaxParHeure,
                gaugeUtilisateursMaxParHeure,
                serieGauge
            );
        }

        private void mettreAJourGauge(String timestamp, String command) {
            String heure = extraireHeure(timestamp);
            int fichiers = editsOuverts.size();
            if (fichiers > gaugeFichiersOuvertsMax) {
                gaugeFichiersOuvertsMax = fichiers;
                heurePicFichiersOuverts = heure;
            }
            mettreAJourMaxParHeure(gaugeFichiersMaxParHeure, heure, fichiers);
            int utilisateurs = compterUtilisateursDansEditsOuverts();
            if (utilisateurs > gaugeUtilisateursOuvertsMax) {
                gaugeUtilisateursOuvertsMax = utilisateurs;
            }
            mettreAJourMaxParHeure(gaugeUtilisateursMaxParHeure, heure, utilisateurs);
            serieGauge.add(new PointGauge(timestamp, command, fichiers, utilisateurs));
        }

        private static void mettreAJourMaxParHeure(Map<String, Integer> parHeure,
                String heure, int valeur) {
            Integer actuel = parHeure.get(heure);
            if (actuel == null || valeur > actuel) {
                parHeure.put(heure, valeur);
            }
        }

        private int compterUtilisateursDansEditsOuverts() {
            Set<String> utilisateurs = new LinkedHashSet<String>();
            for (String cle : editsOuverts.keySet()) {
                int sep = cle.indexOf('|');
                if (sep > 0) {
                    utilisateurs.add(cle.substring(0, sep));
                }
            }
            return utilisateurs.size();
        }

        private void enregistrerSubmitHeure(PerforceEvent evt) {
            String heure = extraireHeure(evt.getTimestamp());
            Set<String> users = submittersParHeure.get(heure);
            if (users == null) {
                users = new LinkedHashSet<String>();
                submittersParHeure.put(heure, users);
            }
            users.add(evt.getUser());
        }

        private void resoudreEditsApresSubmit(PerforceEvent submit) {
            String user = submit.getUser();
            if (submit.getFichiers().isEmpty()) {
                retirerTousLesEditsUtilisateur(user);
                return;
            }
            List<String> aRetirer = new ArrayList<String>();
            String prefixe = user + "|";
            for (Map.Entry<String, PerforceEvent> entree : editsOuverts.entrySet()) {
                if (!entree.getKey().startsWith(prefixe)) {
                    continue;
                }
                String fichierEdit = extraireFichierDeCle(entree.getKey());
                for (String fichierSubmit : submit.getFichiers()) {
                    if (fichiersCorrespondent(fichierEdit, fichierSubmit)) {
                        aRetirer.add(entree.getKey());
                        break;
                    }
                }
            }
            for (String cle : aRetirer) {
                editsOuverts.remove(cle);
            }
        }

        private PerforceEvent retirerParCorrespondanceFloue(String user, String fichier) {
            String prefixe = user + "|";
            String cleTrouvee = null;
            PerforceEvent editTrouve = null;
            for (Map.Entry<String, PerforceEvent> entree : editsOuverts.entrySet()) {
                if (!entree.getKey().startsWith(prefixe)) {
                    continue;
                }
                String fichierEdit = extraireFichierDeCle(entree.getKey());
                if (fichiersCorrespondent(fichierEdit, fichier)) {
                    if (cleTrouvee != null) {
                        return null;
                    }
                    cleTrouvee = entree.getKey();
                    editTrouve = entree.getValue();
                }
            }
            if (cleTrouvee != null) {
                editsOuverts.remove(cleTrouvee);
            }
            return editTrouve;
        }

        private void retirerTousLesEditsUtilisateur(String user) {
            String prefixe = user + "|";
            Iterator<Map.Entry<String, PerforceEvent>> it = editsOuverts.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getKey().startsWith(prefixe)) {
                    it.remove();
                }
            }
        }
    }

    private static final class FiltreCommande extends UnaryFunction<PerforceEvent, Boolean> {
        private final String commande;

        FiltreCommande(String commande) {
            super(PerforceEvent.class, Boolean.class);
            this.commande = commande;
        }

        @Override
        public Boolean getValue(PerforceEvent evt) {
            return commande.equals(evt.getCommande());
        }
    }

    private static final class FiltreAvecDuree extends UnaryFunction<PerforceEvent, Boolean> {
        FiltreAvecDuree() {
            super(PerforceEvent.class, Boolean.class);
        }

        @Override
        public Boolean getValue(PerforceEvent evt) {
            return EnrichissementEvenements.parserDuree(evt.getDuree_s()) != null;
        }
    }

    private static final class FiltreCommandesPhase2 extends UnaryFunction<PerforceEvent, Boolean> {
        FiltreCommandesPhase2() {
            super(PerforceEvent.class, Boolean.class);
        }

        @Override
        public Boolean getValue(PerforceEvent evt) {
            String cmd = evt.getCommande();
            return "user-edit".equals(cmd) || "user-submit".equals(cmd) || "user-revert".equals(cmd);
        }
    }
}
