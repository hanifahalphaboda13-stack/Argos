package ca.uqac.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Summary OpenMetrics sur duration_s (équivalent Prometheus Summary).
 * Quantiles calculés sur l'ensemble du log (batch) ; en production Prometheus
 * utilise souvent une fenêtre glissante.
 */
public final class SummaryDurees {

    public static final double[] QUANTILES_DEFAUT = {0.5, 0.9, 0.95, 0.99};

    public static final class Quantile {
        private final double quantile;
        private final double valeur;

        Quantile(double quantile, double valeur) {
            this.quantile = quantile;
            this.valeur = valeur;
        }

        public double getQuantile() { return quantile; }
        public double getValeur() { return valeur; }

        public String getEtiquette() {
            if (quantile == 0.5) {
                return "p50";
            }
            if (quantile == 0.9) {
                return "p90";
            }
            if (quantile == 0.95) {
                return "p95";
            }
            if (quantile == 0.99) {
                return "p99";
            }
            return "p" + (int) (quantile * 100);
        }
    }

    private final List<Double> valeurs = new ArrayList<Double>();
    private List<Double> valeursTriees;
    private double somme;

    public void observer(double dureeSecondes) {
        if (dureeSecondes < 0) {
            return;
        }
        valeurs.add(dureeSecondes);
        somme += dureeSecondes;
        valeursTriees = null;
    }

    public int getCount() {
        return valeurs.size();
    }

    public double getSum() {
        return somme;
    }

    public double getMoyenne() {
        return valeurs.isEmpty() ? 0 : somme / valeurs.size();
    }

    public double getMax() {
        if (valeurs.isEmpty()) {
            return 0;
        }
        trierSiNecessaire();
        return valeursTriees.get(valeursTriees.size() - 1);
    }

    public double getQuantile(double q) {
        if (valeurs.isEmpty()) {
            return 0;
        }
        if (valeurs.size() == 1) {
            return valeurs.get(0);
        }
        trierSiNecessaire();
        double pos = q * (valeursTriees.size() - 1);
        int bas = (int) Math.floor(pos);
        int haut = (int) Math.ceil(pos);
        if (bas == haut) {
            return valeursTriees.get(bas);
        }
        double poids = pos - bas;
        return valeursTriees.get(bas) * (1 - poids) + valeursTriees.get(haut) * poids;
    }

    public List<Quantile> getQuantiles() {
        return getQuantiles(QUANTILES_DEFAUT);
    }

    public List<Quantile> getQuantiles(double[] quantiles) {
        List<Quantile> resultat = new ArrayList<Quantile>(quantiles.length);
        for (double q : quantiles) {
            resultat.add(new Quantile(q, getQuantile(q)));
        }
        return Collections.unmodifiableList(resultat);
    }

    private void trierSiNecessaire() {
        if (valeursTriees == null) {
            valeursTriees = new ArrayList<Double>(valeurs);
            Collections.sort(valeursTriees);
        }
    }
}
