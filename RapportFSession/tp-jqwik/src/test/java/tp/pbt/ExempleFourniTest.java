package tp.pbt;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exemple fourni — doit passer sans modification.
 * Inspiré de la figure 2 du rapport.
 */
class ExempleFourniTest {

    @Property
    void inverserDeuxFoisRedonneLaListeOriginale(@ForAll List<Integer> liste) {
        List<Integer> resultat = new ArrayList<>(liste);
        Collections.reverse(resultat);
        Collections.reverse(resultat);
        assertEquals(liste, resultat);
    }
}
