package ca.uqac.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Histogram OpenMetrics sur duration_s (équivalent Prometheus Histogram).
 * Buckets cumulatifs : chaque borne compte les observations ≤ le.
 */
public final class HistogrammeDurees {

    /** Bornes en secondes, adaptées aux durées Perforce (ms à ~20 min). */
    public static final double[] BORNES_DEFAUT = {
        0.05, 0.1, 0.25, 0.5, 1, 2, 5, 10, 30, 60, 120, 300, 600, 1200, Double.POSITIVE_INFINITY
    };

    public static final class Bucket {
        private final String le;
        private final int count;

        Bucket(String le, int count) {
            this.le = le;
            this.count = count;
        }

        public String getLe() { return le; }
        public int getCount() { return count; }
    }

    private final double[] bornes;
    private final int[] comptagesCumulatifs;
    private double somme;
    private int total;

    public HistogrammeDurees() {
        this(BORNES_DEFAUT);
    }

    HistogrammeDurees(double[] bornes) {
        this.bornes = bornes.clone();
        this.comptagesCumulatifs = new int[this.bornes.length];
    }

    public void observer(double dureeSecondes) {
        if (dureeSecondes < 0) {
            return;
        }
        total++;
        somme += dureeSecondes;
        for (int i = 0; i < bornes.length; i++) {
            if (dureeSecondes <= bornes[i]) {
                comptagesCumulatifs[i]++;
            }
        }
    }

    public int getCount() { return total; }
    public double getSum() { return somme; }

    public double getMoyenne() {
        return total == 0 ? 0 : somme / total;
    }

    public List<Bucket> getBuckets() {
        List<Bucket> buckets = new ArrayList<Bucket>(bornes.length);
        for (int i = 0; i < bornes.length; i++) {
            buckets.add(new Bucket(formaterBorne(bornes[i]), comptagesCumulatifs[i]));
        }
        return Collections.unmodifiableList(buckets);
    }

    /** Comptage par bucket (non cumulatif, pour affichage console). */
    int[] comptagesParBucket() {
        int[] parBucket = new int[bornes.length];
        int precedent = 0;
        for (int i = 0; i < bornes.length; i++) {
            parBucket[i] = comptagesCumulatifs[i] - precedent;
            precedent = comptagesCumulatifs[i];
        }
        return parBucket;
    }

    double[] getBornes() {
        return bornes.clone();
    }

    private static String formaterBorne(double borne) {
        if (Double.isInfinite(borne)) {
            return "+Inf";
        }
        if (borne == (long) borne) {
            return String.valueOf((long) borne);
        }
        return String.valueOf(borne);
    }
}
