# TP — Property-Based Testing avec jqwik

**Cours :** 8INF958 — Été 2026  
**Durée estimée :** 2 h  
**Prérequis :** Java, JUnit 5, notions de logique du premier ordre (quantificateur universel)

Ce TP s'appuie sur le rapport *Les tests basés sur les propriétés* (`rapport_pbt_jqwik.md`). Vous allez passer du test par exemple au test par propriété, puis appliquer jqwik à une structure de données classique.

---

## Objectifs pédagogiques

À la fin de ce TP, vous devriez être capable de :

1. Formuler une propriété (invariant, inverse, oracle, idempotence) plutôt qu'un cas particulier.
2. Écrire une propriété jqwik avec `@Property` et `@ForAll`.
3. Contraindre la génération (`@Size`, `@IntRange`) et créer un générateur `@Provide`.
4. Interpréter un contre-exemple réduit par le shrinking.
5. Comprendre pourquoi plusieurs propriétés complémentaires sont nécessaires.

---

## Mise en place

Le projet Maven se trouve dans le dossier `tp-jqwik/`.

```bash
cd tp-jqwik
mvn test
```

Les tests marqués **TODO** échouent volontairement tant que vous n'avez pas complété les exercices. Les tests fournis (`ExempleFourniTest`) doivent passer dès le départ.

---

## Partie 1 — Première propriété (20 min)

### Contexte théorique

Un test par exemple vérifie `reverse([1,2,3]) == [3,2,1]`. Une **propriété d'inverse** vérifie plutôt : *pour toute liste L, inverser deux fois L redonne L*.

### Exercice 1.1

Ouvrez `src/test/java/tp/pbt/Partie1_ListePropertiesTest.java` et complétez la méthode `inverserDeuxFoisRedonneLaListeOriginale`.

**Question écrite :** Pourquoi cette propriété est-elle plus générale qu'un test sur `[1, 2, 3]` seul ? (2–3 phrases)

### Exercice 1.2

Complétez `sommeAuMoinsEgaleAuMaximumQuandPasDeNegatifs` en respectant la contrainte `@Size` et `@IntRange` déjà présentes.

**Indice :** si tous les éléments sont positifs ou nuls, la somme est-elle toujours ≥ au maximum ?

---

## Partie 2 — Générateur personnalisé (25 min)

### Contexte

Les générateurs par défaut ne conviennent pas toujours (objets métier, invariants internes). jqwik permet de définir un `Arbitrary` avec `@Provide`.

### Exercice 2.1

Complétez `Partie2_RectanglePropertiesTest.java` :

1. Le générateur `rectangles()` (déjà amorcé).
2. La propriété `permuterLargeurEtHauteurPreserveLAire`.

**Question écrite :** Quel schéma de propriété (section 2.2 du rapport) illustre cet exercice ?

---

## Partie 3 — Arbre binaire de recherche (55 min)

### Contexte

L'implémentation `ArbreBinaireRecherche` expose `inserer`, `contient`, `taille` et `estValide`. **Attention :** l'implémentation contient volontairement un bogue sur la gestion des doublons.

### Exercice 3.1 — Invariant structurel

Complétez `arbreResteValideApresInsertions` dans `Partie3_ArbrePropertiesTest.java`.

Lancez les tests. **Notez** si la propriété échoue et quel contre-exemple minimal jqwik affiche (shrinking).

### Exercice 3.2 — Oracle

Complétez `containsCoherentAvecUneListeDeReference` : comparez `arbre.contient(recherche)` à `valeurs.contains(recherche)`.

### Exercice 3.3 — Idempotence

Complétez `insertionRepeteeNAugmentePasLaTaille` : insérer deux fois la même valeur ne doit pas augmenter `taille()`.

### Exercice 3.4 — Analyse

Répondez par écrit :

1. Quel est le contre-exemple minimal trouvé pour l'invariant structurel ?
2. Corrigez le bogue dans `ArbreBinaireRecherche.java` (méthode `insererRec`).
3. Relancez `mvn test` : toutes les propriétés doivent passer.
4. Pourquoi la propriété sur la taille (3.3) détecte-t-elle le bogue plus directement que certaines autres ?

---

## Partie 4 — Réflexion (20 min)

Répondez brièvement (5–8 lignes chacune) :

1. **PBT vs test par exemple :** quelle garantie offre jqwik après 1000 essais réussis ? Quelle garantie *n'offre-t-il pas* ?
2. **Shrinking :** à quoi sert-il concrètement pour le débogage ?
3. **Complémentarité :** d'après le rapport (section 6), dans quel contexte préféreriez-vous ACTS plutôt que jqwik ?

---

## Barème indicatif

| Partie | Points |
|--------|--------|
| 1 — Listes | /15 |
| 2 — Rectangle | /15 |
| 3 — ABR (propriétés + correction) | /40 |
| 4 — Réflexion | /20 |
| Qualité du code et exécution `mvn test` | /10 |

---

## Livrables

1. Code complété et bogue corrigé.
2. Un fichier `reponses.md` avec vos réponses aux questions écrites (parties 1, 2, 3.4 et 4).

---

## Ressources

- Rapport du cours : `rapport_pbt_jqwik.md`
- [jqwik User Guide](https://jqwik.net/docs/current/user-guide.html)
- Tutoriel du cours : *How To Solve It! In Java!*
