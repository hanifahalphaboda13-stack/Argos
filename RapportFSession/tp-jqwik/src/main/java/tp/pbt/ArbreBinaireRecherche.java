package tp.pbt;

public class ArbreBinaireRecherche<T extends Comparable<T>> {

    private Noeud<T> racine;
    private int taille = 0;

    private static class Noeud<T> {
        T valeur;
        Noeud<T> gauche, droite;

        Noeud(T valeur) {
            this.valeur = valeur;
        }
    }

    public void inserer(T valeur) {
        racine = insererRec(racine, valeur);
    }

    private Noeud<T> insererRec(Noeud<T> noeud, T valeur) {
        if (noeud == null) {
            taille++;
            return new Noeud<>(valeur);
        }
        int cmp = valeur.compareTo(noeud.valeur);
        if (cmp < 0) {
            noeud.gauche = insererRec(noeud.gauche, valeur);
        } else if (cmp > 0) {
            noeud.droite = insererRec(noeud.droite, valeur);
        }
        // cmp == 0 : valeur déjà présente, on ignore l'insertion
        return noeud;
    }

    public boolean contient(T valeur) {
        Noeud<T> courant = racine;
        while (courant != null) {
            int cmp = valeur.compareTo(courant.valeur);
            if (cmp == 0) {
                return true;
            }
            courant = cmp < 0 ? courant.gauche : courant.droite;
        }
        return false;
    }

    public int taille() {
        return taille;
    }

    public boolean estValide() {
        return estValideRec(racine, null, null);
    }

    private boolean estValideRec(Noeud<T> noeud, T min, T max) {
        if (noeud == null) {
            return true;
        }
        if (min != null && noeud.valeur.compareTo(min) <= 0) {
            return false;
        }
        if (max != null && noeud.valeur.compareTo(max) >= 0) {
            return false;
        }
        return estValideRec(noeud.gauche, min, noeud.valeur)
                && estValideRec(noeud.droite, noeud.valeur, max);
    }
}
