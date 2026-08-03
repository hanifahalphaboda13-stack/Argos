package tp.pbt;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partie 3 — Arbre binaire de recherche.
 * L'implémentation contient un bogue volontaire sur les doublons.
 */
class Partie3_ArbrePropertiesTest {

    private ArbreBinaireRecherche<Integer> construireArbre(List<Integer> valeurs) {
        ArbreBinaireRecherche<Integer> arbre = new ArbreBinaireRecherche<>();
        valeurs.forEach(arbre::inserer);
        return arbre;
    }

    @Property
    void arbreResteValideApresInsertions(@ForAll List<Integer> valeurs) {
        ArbreBinaireRecherche<Integer> arbre = construireArbre(valeurs);
        assertTrue(arbre.estValide());
    }

    @Property
    void containsCoherentAvecUneListeDeReference(
            @ForAll List<Integer> valeurs, @ForAll Integer recherche) {
        ArbreBinaireRecherche<Integer> arbre = construireArbre(valeurs);
        assertEquals(valeurs.contains(recherche), arbre.contient(recherche));
    }

    @Property
    void insertionRepeteeNAugmentePasLaTaille(
            @ForAll Integer valeur, @ForAll List<Integer> valeurs) {
        ArbreBinaireRecherche<Integer> arbre = construireArbre(valeurs);
        arbre.inserer(valeur);
        int tailleApresPremiereInsertion = arbre.taille();
        arbre.inserer(valeur);
        assertEquals(tailleApresPremiereInsertion, arbre.taille());
    }
}
