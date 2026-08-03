package ca.uqac.detection;

import java.util.List;

public class Main {

    /** Répertoire racine du projet (log + exports). */
    private static final String REPERTOIRE_PROJET =
        "/home/hani/eclipse-workspace/PerforceLog";

    private static final String LOG_DEFAUT =
        REPERTOIRE_PROJET + "/log.txt";

    private static final String FICHIER_JSON =
        REPERTOIRE_PROJET + "/perforce_events_v3.json";

    private static final String FICHIER_CSV =
        REPERTOIRE_PROJET + "/perforce_events_v3.csv";

    private static final String FICHIER_ALERTES =
        REPERTOIRE_PROJET + "/perforce_events_alertes.json";

    private static final String FICHIER_GAUGES =
        REPERTOIRE_PROJET + "/perforce_gauges.json";

    private static final String FICHIER_HISTOGRAMMES =
        REPERTOIRE_PROJET + "/perforce_histogrammes.json";

    private static final String FICHIER_SUMMARIES =
        REPERTOIRE_PROJET + "/perforce_summaries.json";

    private static final String FICHIER_COUNTERS =
        REPERTOIRE_PROJET + "/perforce_counters.json";

    public static void main(String[] args) throws Exception {
        String logBrut;
        int seuilRush = UtilEvenements.SEUIL_RUSH_DEFAUT;

        if (args.length < 1) {
            logBrut = LOG_DEFAUT;
            System.out.println("Usage : java Main [<log.txt>] [seuilRush]");
            System.out.println("Défaut : " + LOG_DEFAUT + "\n");
        } else {
            logBrut = args[0];
            if (args.length >= 2) {
                seuilRush = Integer.parseInt(args[1]);
            }
        }

        System.out.println("Lecture du log : " + logBrut);
        List<PerforceEvent> evenements = LecteurLog.lire(logBrut);
        System.out.println("Événements extraits : " + evenements.size());

        System.out.println("\n--- Statistiques doublons ---");
        UtilEvenements.afficherStatistiquesDoublons(evenements);

        List<PerforceEvent> uniques = UtilEvenements.supprimerDoublons(evenements);
        System.out.println("Événements uniques     : " + uniques.size());

        // Export v3 aligné sur perforce_events_v3 (événements sans doublons)
        List<EnrichissementEvenements.LigneAnalytique> lignes =
            EnrichissementEvenements.enrichir(uniques);

        // --- Export événements : décommenter json et/ou csv (format perforce_events_v3) ---
        EnrichissementEvenements.exporterLignes(lignes, FICHIER_JSON, UtilEvenements.FormatSortie.JSON);
        System.out.println("Fichier écrit (json) : " + FICHIER_JSON);

        EnrichissementEvenements.exporterLignes(lignes, FICHIER_CSV, UtilEvenements.FormatSortie.CSV);
        System.out.println("Fichier écrit (csv) : " + FICHIER_CSV);

        UtilEvenements.ResultatPhase1 phase1 = UtilEvenements.analyserAvecBeepBeep(uniques);
        UtilEvenements.exporterCompteurs(phase1, FICHIER_COUNTERS);
        System.out.println("Counters écrits (json) : " + FICHIER_COUNTERS);
        UtilEvenements.exporterGauges(phase1.getGauges(), FICHIER_GAUGES);
        System.out.println("Gauges écrites (json) : " + FICHIER_GAUGES);
        UtilEvenements.exporterHistogrammes(phase1.getHistogrammes(), FICHIER_HISTOGRAMMES);
        System.out.println("Histogrammes écrits (json) : " + FICHIER_HISTOGRAMMES);
        UtilEvenements.exporterSummaries(phase1.getSummaries(), FICHIER_SUMMARIES);
        System.out.println("Summaries écrits (json) : " + FICHIER_SUMMARIES);

        UtilEvenements.ResultatPhase2 alertes = UtilEvenements.analyserPhase2(uniques, seuilRush);

        // --- Export alertes (phase 2) ---
        UtilEvenements.exporterAlertesPhase2(alertes, FICHIER_ALERTES);
        System.out.println("Alertes écrites (json) : " + FICHIER_ALERTES);

        GenererGraphiques.generer(REPERTOIRE_PROJET, uniques, phase1, alertes);

        System.out.println("\nAperçu (20 premiers événements) :");
        for (int i = 0; i < 20 && i < lignes.size(); i++) {
            System.out.println("\nÉvénement " + (i + 1) + " :");
            EnrichissementEvenements.afficherDetail(lignes.get(i));
        }
    }
}
