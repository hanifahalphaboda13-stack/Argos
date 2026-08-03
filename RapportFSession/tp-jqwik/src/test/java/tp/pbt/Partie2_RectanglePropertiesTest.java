package tp.pbt;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partie 2 — Générateur personnalisé avec @Provide.
 */
class Partie2_RectanglePropertiesTest {

    @Provide
    Arbitrary<Rectangle> rectangles() {
        Arbitrary<Integer> largeurs = Arbitraries.integers().between(1, 1000);
        Arbitrary<Integer> hauteurs = Arbitraries.integers().between(1, 1000);
        return Combinators.combine(largeurs, hauteurs)
                .as((l, h) -> new Rectangle(l, h));
    }

    @Property
    void laireEstToujoursPositive(@ForAll("rectangles") Rectangle r) {
        assertTrue(r.aire() > 0);
    }

    @Property
    void permuterLargeurEtHauteurPreserveLAire(@ForAll("rectangles") Rectangle r) {
        Rectangle permute = new Rectangle(r.getHauteur(), r.getLargeur());
        assertEquals(r.aire(), permute.aire());
    }
}
