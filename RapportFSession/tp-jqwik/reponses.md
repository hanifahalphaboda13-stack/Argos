# Réponses — TP Property-Based Testing

**Nom :** Hanifah ALPHA-BODA & Abdoul KANDE  
**Date :** 6 juillet 2026

---

## Partie 1

### Question 1.1

Un test sur `[1, 2, 3]` ne vérifie qu'une seule instance du comportement attendu. La propriété d'inverse quantifie universellement sur toutes les listes du domaine généré : jqwik en teste des milliers (listes vides, singletons, doublons, grandes listes) et un seul contre-exemple suffit à réfuter la propriété. C'est l'équivalent pratique d'un ∀x P(x) en logique du premier ordre, échantillonné statistiquement.

---

## Partie 2

### Question 2.1

Il s'agit d'une **propriété métamorphique** (ou d'invariance sous transformation) : permuter largeur et hauteur est une transformation de l'entrée qui ne doit pas modifier le résultat (l'aire). On peut aussi la relier à la **commutativité** de la multiplication.

---

## Partie 3.4

1. **Contre-exemple minimal (invariant structurel) :** `[0, 0]` — deux insertions successives de la même valeur violent l'invariant d'ordre strict de l'ABR.

2. **Correction apportée dans `ArbreBinaireRecherche` :** dans `insererRec`, lorsque `cmp == 0` (valeur déjà présente), on ne réinsère plus dans le sous-arbre droit et on n'incrémente plus `taille`. L'insertion est ignorée, conformément à la spécification d'un ABR sans doublons.

3. **Pourquoi la propriété d'idempotence (3.3) détecte le bogue plus directement ?** Elle cible explicitement le comportement attendu sur les doublons : insérer deux fois la même valeur ne doit pas changer `taille()`. L'invariant structurel détecte aussi le bogue, mais de façon indirecte (violation de l'ordre). L'oracle `contains` peut rester cohérent même avec un arbre invalide si la valeur est trouvée malgré tout.

---

## Partie 4 — Réflexion

### 1. PBT vs test par exemple

Après 1000 essais réussis, jqwik offre une **confiance statistique** : aucun contre-exemple n'a été trouvé parmi un échantillon large, biaisé vers les cas limites. En revanche, cela **ne prouve pas** que la propriété est vraie pour toutes les entrées possibles — un domaine infini peut toujours contenir des régions non visitées. Le PBT reste une méthode de test, pas une preuve formelle.

### 2. Shrinking

Le shrinking simplifie automatiquement un contre-exemple aléatoire complexe (ex. une longue liste) jusqu'au cas minimal qui reproduit encore l'échec (ex. `[0, 0]`). Cela accélère le diagnostic : le développeur obtient un scénario lisible et reproductible, idéal pour écrire ensuite un test de régression classique.

### 3. ACTS vs jqwik

ACTS convient aux **espaces de configuration discrets et finis** : combinaisons d'options logicielles, OS × navigateur × version JVM, etc. Il garantit une couverture combinatoire systématique (pairwise, n-wise) sur un plan explicite. jqwik est préférable pour des **données riches et potentiellement infinies** : entiers sur une large plage, listes de taille variable, structures récursives. Les deux approches sont complémentaires : ACTS pour la configuration, jqwik pour les données métier.
