package tp.pbt;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partie 1 — Propriétés sur les listes.
 */
class Partie1_ListePropertiesTest {

    @Property
    void inverserDeuxFoisRedonneLaListeOriginale(@ForAll List<Integer> liste) {
        List<Integer> resultat = new ArrayList<>(liste);
        Collections.reverse(resultat);
        Collections.reverse(resultat);
        assertEquals(liste, resultat);
    }

    @Property
    void sommeAuMoinsEgaleAuMaximumQuandPasDeNegatifs(
            @ForAll @Size(min = 1, max = 50) List<@IntRange(min = 0, max = 100) Integer> liste) {
        int somme = liste.stream().mapToInt(Integer::intValue).sum();
        int maximum = Collections.max(liste);
        assertTrue(somme >= maximum);
    }
}
