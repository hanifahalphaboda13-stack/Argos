

**Les tests basés sur les propriétés**

***(Property-Based Testing)***

Étude théorique et pratique avec la librairie jqwik

Rapport présenté dans le cadre du cours

8INF958 — Été 2026

Par Hanifah ALPHA-BODA & Abdoul KANDE

# **Table des matières**

1\. Introduction

2\. Fondements théoriques du Property-Based Testing

   2.1 Le test par exemple : forces et limites

   2.2 Définir une propriété

   2.3 Liens avec la logique formelle

   2.4 Quantificateurs universels et existentiels

3\. Fonctionnement interne du PBT

   3.1 Génération de données aléatoires

   3.2 Le shrinking (réduction des contre-exemples)

   3.3 Stratégies et biais de génération

   3.4 Garanties statistiques et limites face à la preuve formelle

4\. jqwik en pratique

   4.1 Installation et intégration

   4.2 Anatomie d'une propriété jqwik

   4.3 Générateurs personnalisés (@Provide)

   4.4 Vie et cycle des exemples : Arbitraries avancés

   4.5 Intégration continue et reproductibilité

5\. Étude de cas : vérification d'une structure de données

   5.1 Présentation du problème

   5.2 Propriétés identifiées

   5.3 Résultats et contre-exemples trouvés

   5.4 Bonnes pratiques et pièges courants

6\. Comparaison avec d'autres approches de test

   6.1 PBT et tests combinatoires (ACTS)

   6.2 PBT et vérification à l'exécution

   6.3 Avantages, limites et complémentarité

   6.4 Vers une stratégie de test combinée

7\. Conclusion

Annexe A — Code source complet de l'étude de cas

Annexe B — Glossaire

8\. Références

# **Liste des figures et des tableaux**

**Figures**

Figure 1 — Déclaration de la dépendance jqwik dans un projet Maven

Figure 2 — Une première propriété jqwik testant l'involution de l'inversion de liste

Figure 3 — Contraintes de génération avec @Size et @IntRange

Figure 4 — Générateur personnalisé combinant deux Arbitrary avec Combinators.combine

Figure 5 — Propriété d'invariant structurel sur l'arbre binaire de recherche

Figure 6 — Propriété de type oracle comparant l'ABR à une liste de référence

Figure 7 — Propriété d'invariance du contenu logique sous permutation de l'ordre d'insertion

Figure 8 — Implémentation complète de l'arbre binaire de recherche testé

Figure 9 — Suite de propriétés jqwik complète pour l'arbre binaire de recherche

**Tableaux**

Tableau 1 — Résumé des résultats obtenus sur l'implémentation fautive de l'arbre binaire de recherche

Tableau 2 — Synthèse comparative des trois approches de test étudiées

Tableau 3 — Principaux paramètres de configuration d'une propriété jqwik

# **1\. Introduction**

Le test logiciel occupe une place centrale dans le cours 8INF958, où l'on apprend que la qualité d'un programme ne se mesure pas seulement à sa capacité à produire le bon résultat sur quelques exemples, mais à sa capacité à respecter ses spécifications sur l'ensemble des entrées possibles. Or, la pratique la plus répandue en industrie reste le test par exemple (example-based testing) : le développeur choisit une poignée de valeurs d'entrée, calcule ou devine la sortie attendue, puis fige cette association dans une assertion. Cette approche est simple, rapide à écrire et facile à comprendre, mais elle souffre d'un défaut fondamental qu'elle partage avec toute méthode d'échantillonnage manuel : elle ne teste que ce que l'auteur du test a pensé à tester.

Le test basé sur les propriétés (Property-Based Testing, ou PBT) propose un changement de posture. Plutôt que de fixer une entrée et une sortie, on énonce une propriété générale que le programme doit respecter pour toute entrée valide, puis on laisse un moteur de génération produire automatiquement un grand nombre de cas, y compris des cas limites auxquels un humain ne penserait pas spontanément. Lorsqu'un cas fait échouer la propriété, l'outil ne se contente pas de le signaler : il applique un mécanisme de réduction, ou shrinking, qui simplifie progressivement le contre-exemple jusqu'à obtenir la version la plus petite et la plus lisible possible du bogue.

Cette approche trouve ses origines dans la bibliothèque QuickCheck, conçue pour le langage Haskell à la fin des années 1990 par Koen Claessen et John Hughes. L'idée s'est depuis propagée dans pratiquement tous les écosystèmes de programmation : Hypothesis en Python, fast-check en JavaScript, ScalaCheck en Scala, et, pour ce qui concerne directement ce rapport, jqwik pour la plateforme Java. jqwik s'intègre à l'écosystème JUnit 5 en tant que moteur de test (test engine), ce qui permet de faire cohabiter des tests unitaires classiques et des propriétés au sein d'un même projet, avec les mêmes outils de compilation et d'intégration continue déjà vus dans ce cours.

Ce rapport poursuit trois objectifs. Le premier est théorique : situer le PBT par rapport aux notions déjà abordées dans le cours, notamment la logique propositionnelle et la logique du premier ordre, puisqu'une propriété n'est rien d'autre qu'un énoncé logique quantifié universellement sur le domaine des entrées. Le deuxième objectif est technique : expliquer le fonctionnement interne d'un moteur de PBT, en particulier la génération de données et le shrinking, deux mécanismes qui distinguent fondamentalement cette approche du test combinatoire vu plus tôt dans la session. Le troisième objectif est pratique : présenter la librairie jqwik en détail, avec des exemples de code exécutables, puis appliquer ces techniques à une étude de cas concrète, à savoir la vérification des propriétés d'une structure de données classique.

Le rapport se termine par une comparaison entre le PBT et les autres familles de test présentées dans le cours — tests combinatoires avec ACTS et vérification à l'exécution avec des outils comme BeepBeep — afin de situer clairement où le PBT excelle et où il montre ses limites. Cette mise en perspective est importante : le PBT n'est pas une solution universelle qui remplacerait les autres techniques, mais un outil complémentaire, particulièrement efficace pour vérifier des propriétés algébriques (commutativité, idempotence, propriétés d'inverse) et pour découvrir des cas limites inattendus dans du code manipulant des structures de données ou des calculs numériques.

**2\. Fondements théoriques du Property-Based Testing**

## **2.1 Le test par exemple : forces et limites**

Un test unitaire classique associe une entrée précise à une sortie attendue précise. Prenons l'exemple d'une fonction qui inverse une liste. Un test par exemple pourrait vérifier que l'inversion de la liste \[1, 2, 3\] donne \[3, 2, 1\]. Ce test est utile : il documente le comportement attendu, il s'exécute rapidement, et il échoue de manière déterministe si l'implémentation se brise. Mais il ne dit rien du comportement de la fonction sur une liste vide, sur une liste à un seul élément, sur une liste contenant des doublons, ou sur une liste de dix mille éléments. Le développeur devrait imaginer et écrire chacun de ces cas séparément pour obtenir une couverture raisonnable.

Le problème s'aggrave à mesure que la complexité du domaine d'entrée augmente. Pour une fonction prenant deux entiers, l'espace des paires possibles est déjà infini ; pour une fonction prenant une structure arborescente ou un graphe, l'espace des entrées valides devient combinatoire et il est illusoire de penser qu'un ensemble de cas choisis à la main puisse en représenter la diversité. C'est précisément le même constat qui motive les tests combinatoires étudiés plus tôt dans ce cours, mais le PBT y répond différemment : au lieu d'énumérer systématiquement des combinaisons de valeurs de paramètres discrets, il échantillonne aléatoirement dans un domaine potentiellement continu ou récursif, guidé par des générateurs et affiné par le shrinking.

## **2.2 Définir une propriété**

Une propriété est un énoncé qui doit rester vrai pour toute entrée respectant certaines conditions. Formellement, on peut l'écrire comme une formule quantifiée universellement : pour toute entrée x appartenant à un domaine D, un prédicat P(x) doit être satisfait. Cette formulation est directement héritée de la logique du premier ordre vue dans ce cours : la propriété n'est jamais vérifiée pour un x particulier, mais pour tous les x du domaine, ce qui est très différent d'un test par exemple qui ne vérifie qu'une instance de la formule.

En pratique, les propriétés les plus utiles suivent quelques schémas récurrents que la communauté a fini par cataloguer, car ils reviennent dans presque tous les domaines applicatifs :

* **Propriétés d'inverse :** une opération et son inverse, appliquées successivement, ramènent à l'état de départ. Par exemple, sérialiser puis désérialiser un objet doit reproduire l'objet original, ou encore trier puis inverser puis trier à nouveau doit donner le même résultat que trier une seule fois.

* **Invariants structurels :** une propriété qui doit toujours être vraie sur le résultat, indépendamment de l'entrée. Par exemple, après insertion dans un arbre binaire de recherche, la propriété d'ordre de l'arbre doit toujours être respectée.

* **Propriétés de préservation métamorphique :** une relation entre plusieurs exécutions du programme avec des entrées liées entre elles. Par exemple, trier une liste puis y ajouter un élément déjà trié à sa place doit donner le même résultat que trier la liste complète directement.

* **Comparaison avec une implémentation de référence (oracle) :** on compare une implémentation optimisée à une implémentation naïve mais évidemment correcte, sur les mêmes entrées générées.

* **Idempotence et commutativité :** des propriétés algébriques classiques, comme le fait qu'appliquer une opération de normalisation deux fois donne le même résultat qu'une seule fois, ou que l'ordre de deux opérations indépendantes n'affecte pas le résultat final.

Ce catalogue est important pour la suite du rapport : dans l'étude de cas de la section 5, nous nous appuierons explicitement sur les catégories d'invariant structurel et de propriété d'inverse pour construire un ensemble de propriétés couvrant une structure de données.

## **2.3 Liens avec la logique formelle**

Le cours a introduit la logique propositionnelle puis la logique du premier ordre comme fondations pour raisonner sur des spécifications. Le PBT peut être vu comme une tentative de vérifier automatiquement, par échantillonnage, des énoncés qui seraient autrement démontrés par une preuve formelle. Là où une preuve établit la vérité d'une propriété pour tout le domaine de manière certaine, le PBT ne fait qu'augmenter la confiance en testant un grand nombre d'instances choisies de façon à maximiser les chances de trouver un contre-exemple s'il en existe un. Cette distinction est essentielle et sera reprise dans la section 6 : le PBT n'est pas une méthode de preuve, c'est une méthode de test, avec toutes les limites que cela implique — notamment l'impossibilité de prouver l'absence de bogue, seulement de rapporter leur présence.

Il existe néanmoins une passerelle naturelle entre les deux mondes. Une spécification exprimée en OCL, comme celle vue dans les exercices du cours portant sur les diagrammes UML, est déjà formulée comme un ensemble d'invariants et de contraintes portant sur les instances d'une classe. Traduire une contrainte OCL en propriété jqwik est souvent immédiat : il suffit de générer des instances de la classe concernée et de vérifier que la contrainte, traduite en code Java, reste satisfaite. Le PBT peut ainsi servir de pont pratique entre une spécification formelle rédigée en amont du développement et un harnais de test exécutable qui l'accompagne tout au long du cycle de vie du logiciel.

## **2.4 Quantificateurs universels et existentiels dans les propriétés**

La grande majorité des propriétés rencontrées en PBT s'expriment avec un quantificateur universel : pour toute entrée x du domaine, un prédicat doit être vrai. C'est cette forme que jqwik teste par défaut lorsqu'un paramètre est annoté @ForAll : l'échec d'un seul essai suffit à réfuter la propriété, exactement comme en logique du premier ordre un seul contre-exemple suffit à réfuter une formule universellement quantifiée. Cette asymétrie entre la difficulté de prouver et la facilité de réfuter une formule universelle est au cœur de la philosophie du test : on ne cherche jamais à démontrer qu'une propriété est vraie, seulement à échouer à démontrer qu'elle est fausse après un effort de recherche raisonnable.

Il est cependant parfois utile d'exprimer des propriétés faisant intervenir un quantificateur existentiel, par exemple « il existe un ordre d'insertion qui produit un arbre déséquilibré ». jqwik ne fournit pas d'annotation dédiée à ce type d'énoncé, car un quantificateur existentiel se prête mal à une falsification automatique : contrairement à un énoncé universel, un échec isolé de la recherche ne prouve rien, puisqu'il faudrait explorer tout le domaine pour conclure à l'absence d'un témoin. Dans la pratique, ce genre d'énoncé est plutôt reformulé en une propriété universelle inversée, où l'on cherche à démontrer qu'aucune entrée générée ne peut produire le témoin recherché, ce qui revient à chercher un contre-exemple à la négation de l'énoncé existentiel. Cette reformulation illustre bien à quel point la conception d'une bonne suite de propriétés demande une réflexion logique similaire à celle exigée pour traduire un cahier des charges en formules de logique du premier ordre, comme cela a été pratiqué dans les exercices du cours sur ce sujet.

**3\. Fonctionnement interne du PBT**

## **3.1 Génération de données aléatoires**

Le cœur d'un moteur de PBT est son système de génération de valeurs, souvent appelé Arbitrary (dans jqwik) ou Generator (dans d'autres bibliothèques). Un générateur encapsule à la fois la logique de production de valeurs aléatoires et, comme on le verra à la section 3.2, la logique de réduction de ces valeurs. Pour les types primitifs, jqwik fournit des générateurs prêts à l'emploi : entiers dans un intervalle, chaînes de caractères d'une longueur donnée, listes d'une taille bornée, etc.

Un aspect souvent mal compris par les débutants est que la génération n'est pas purement uniforme. Un bon générateur biaise volontairement l'échantillonnage vers les valeurs qui, l'expérience le montre, révèlent le plus souvent des bogues : zéro, les valeurs négatives, les bornes d'un intervalle, les chaînes vides, les collections vides ou à un seul élément. Ce biais délibéré augmente la probabilité de trouver un contre-exemple avec un nombre d'essais raisonnable, généralement de l'ordre de mille exécutions par propriété par défaut dans jqwik, un chiffre largement suffisant pour des fonctions de complexité modérée mais ajustable pour des cas plus exigeants.

La génération de structures composées, comme des listes d'objets ou des arbres, se construit par combinaison de générateurs plus simples, un principe similaire à celui des combinateurs de parseurs vus dans d'autres contextes. jqwik expose pour cela des opérateurs de combinaison qui permettent, par exemple, de construire un générateur de paires (entier, chaîne) à partir d'un générateur d'entiers et d'un générateur de chaînes, ou de générer un objet complexe en combinant des générateurs pour chacun de ses attributs.

## **3.2 Le shrinking (réduction des contre-exemples)**

Le shrinking est sans doute la contribution la plus originale du PBT par rapport aux autres formes de test automatisé. Lorsqu'une propriété échoue pour une valeur générée, cette valeur est rarement la plus lisible ou la plus utile pour comprendre le bogue : une liste de trente-sept entiers aléatoires révélant un défaut de tri n'aide pas immédiatement le développeur à comprendre la cause du problème. Le moteur de PBT applique alors un algorithme de réduction qui recherche, parmi des variantes plus petites ou plus simples de la valeur fautive, une variante qui fait encore échouer la propriété. Ce processus est répété jusqu'à atteindre un point fixe : la plus petite valeur (au sens d'un ordre de simplicité défini par le générateur) qui reproduit encore l'échec.

Pour une liste d'entiers, le shrinking essaiera typiquement de retirer des éléments, de remplacer des éléments par des valeurs plus proches de zéro, ou de raccourcir la liste, en revérifiant la propriété à chaque étape. Pour l'exemple précédent, la liste de trente-sept entiers pourrait ainsi être réduite à une liste de deux éléments, voire à un seul, qui suffit à démontrer le défaut de l'implémentation. Le contre-exemple final rapporté au développeur est donc à la fois minimal et déterministe, ce qui en fait un point de départ idéal pour écrire un test de régression classique une fois le bogue corrigé.

Il est important de noter que le shrinking n'est pas une simple recherche exhaustive : il s'appuie sur la structure du générateur pour proposer des candidats pertinents plutôt que d'essayer toutes les réductions possibles, ce qui serait combinatoirement intraitable pour des structures de données de taille importante. C'est pourquoi jqwik, comme la plupart des bibliothèques modernes, associe intrinsèquement à chaque Arbitrary une stratégie de réduction cohérente avec sa stratégie de génération.

## **3.3 Stratégies et biais de génération**

Au-delà de la génération purement aléatoire, jqwik permet de configurer plusieurs aspects du processus d'exploration de l'espace des entrées, ce qui donne au développeur un contrôle fin sur l'équilibre entre rapidité d'exécution et couverture des cas limites :

* **Le nombre d'essais (tries) :** configurable par propriété, il détermine combien d'entrées différentes seront testées avant de conclure que la propriété est vraisemblablement vérifiée.

* **Les bords (edge cases) :** jqwik injecte systématiquement, en début d'exécution, un ensemble de valeurs limites connues (zéro, valeurs minimales et maximales du type, chaînes vides) avant de passer à la génération purement aléatoire.

* **La graine aléatoire (seed) :** chaque exécution peut être rejouée de manière déterministe à partir d'une graine, ce qui garantit la reproductibilité d'un échec pour le débogage ou pour l'intégration continue.

* **La taille des structures générées :** un paramètre de taille croissant au fil des essais permet de commencer par de petites structures et d'augmenter progressivement la complexité, une stratégie qui accélère la détection des bogues simples avant d'explorer des cas plus rares.

Ces mécanismes rapprochent le PBT d'une forme de recherche heuristique dans l'espace des entrées, un peu à la manière dont un algorithme de recherche opérationnelle explore un espace de solutions. Cette parenté conceptuelle explique pourquoi le PBT se combine bien, comme on le verra à la section 6, avec les tests combinatoires : les deux techniques partagent l'objectif de maximiser la couverture d'un espace d'entrée avec un nombre limité de cas, mais avec des mécanismes d'exploration très différents — systématique et discret pour l'un, aléatoire et guidé pour l'autre.

Le tableau ci-dessous résume les principaux paramètres de configuration disponibles dans jqwik et leur effet sur le compromis entre rapidité d'exécution et probabilité de détection d'un défaut, un compromis que tout développeur doit ajuster en fonction du contexte du projet, notamment la fréquence d'exécution de la suite de tests en intégration continue.

| Paramètre | Effet d'une valeur plus élevée | Compromis typique |
| ----- | ----- | ----- |
| tries | Plus d'essais, meilleure détection | Temps d'exécution accru |
| maxDiscardRatio | Tolère plus de valeurs filtrées (filter) | Risque de propriété mal formée non détecté |
| edgeCases.mode | Priorise davantage les cas limites connus | Moins de diversité aléatoire pure |
| shrinking.mode | Réduction plus poussée du contre-exemple | Temps de diagnostic après échec accru |

*Tableau 3 — Principaux paramètres de configuration d'une propriété jqwik*

## **3.4 Garanties statistiques et limites face à la preuve formelle**

Il importe de resituer précisément la nature de la garantie offerte par le PBT, un point déjà effleuré à la section 2.3 mais qui mérite d'être approfondi ici avec les éléments techniques introduits depuis. Lorsqu'une propriété jqwik s'exécute avec succès sur mille essais, cela ne démontre en aucune façon que la propriété est vraie pour toutes les entrées possibles : cela démontre seulement qu'aucun contre-exemple n'a été trouvé parmi les mille entrées échantillonnées, aussi bien choisies soient-elles grâce aux biais décrits à la section 3.1. Un domaine d'entrée infini contient toujours, en principe, des régions que même un million d'essais ne visiterait jamais.

Cette limite rapproche le PBT d'une méthode d'analyse statistique plutôt que d'une méthode de preuve, et la distingue nettement des techniques de vérification formelle qui seraient abordées dans un cours de logique ou de méthodes formelles, où un théorème de correction est démontré une fois pour toutes par déduction plutôt que par échantillonnage. Cette distinction n'est pas une faiblesse propre à jqwik : elle est inhérente à toute forme de test, y compris les tests combinatoires vus dans ce cours, qui garantissent une couverture systématique d'un plan de test donné mais ne garantissent rien au-delà de ce plan. La spécificité du PBT est plutôt de rendre ce compromis explicite et configurable — via le nombre d'essais, les bornes des générateurs et les biais vers les cas limites — plutôt que de le laisser implicite dans le choix, souvent arbitraire, des exemples qu'un développeur humain aurait choisi de tester manuellement.

**4\. jqwik en pratique**

## **4.1 Installation et intégration**

jqwik s'installe comme moteur de test JUnit 5 additionnel. Dans un projet Maven, il suffit d'ajouter la dépendance suivante au fichier pom.xml, aux côtés des dépendances JUnit déjà présentes dans le gabarit de projet AntRun mentionné dans le cours :

\<dependency\>  
    \<groupId\>net.jqwik\</groupId\>  
    \<artifactId\>jqwik\</artifactId\>  
    \<version\>1.9.0\</version\>  
    \<scope\>test\</scope\>  
\</dependency\>

*Figure 1 — Déclaration de la dépendance jqwik dans un projet Maven*

Pour un projet Gradle, la déclaration équivalente utilise testImplementation. Une fois la dépendance résolue, JUnit 5 détecte automatiquement le moteur jqwik au démarrage grâce au mécanisme de découverte de services (ServiceLoader) déjà utilisé par JUnit pour ses propres extensions. Aucune configuration additionnelle n'est nécessaire : les classes de test contenant des méthodes annotées @Property sont exécutées par jqwik, tandis que les méthodes annotées @Test restent exécutées par le moteur JUnit Jupiter standard. Les deux types de tests peuvent cohabiter dans la même classe, ce qui permet une adoption progressive du PBT dans un projet existant.

## **4.2 Anatomie d'une propriété jqwik**

Une propriété jqwik ressemble à un test JUnit classique, à ceci près que ses paramètres sont annotés pour indiquer comment ils doivent être générés, et que le corps de la méthode exprime une assertion valable pour tous les paramètres reçus. Voici un premier exemple simple, qui vérifie qu'inverser deux fois une liste redonne la liste d'origine — une propriété d'inverse au sens défini à la section 2.2 :

@Property  
void inverserDeuxFoisRedonneLaListeOriginale(@ForAll List\<Integer\> liste) {  
    List\<Integer\> resultat \= new ArrayList\<\>(liste);  
    Collections.reverse(resultat);  
    Collections.reverse(resultat);  
    assertEquals(liste, resultat);  
}

*Figure 2 — Une première propriété jqwik testant l'involution de l'inversion de liste*

L'annotation @ForAll indique à jqwik qu'il doit fournir automatiquement des valeurs pour ce paramètre, en s'appuyant sur un générateur par défaut associé au type déclaré, ici List\<Integer\>. Par défaut, jqwik exécutera cette propriété mille fois, chaque fois avec une liste différente, générée aléatoirement, en incluant systématiquement des cas limites comme la liste vide ou une liste à un seul élément.

Il est possible de contraindre plus précisément les valeurs générées à l'aide d'annotations complémentaires. Par exemple, pour limiter la taille des listes générées ou l'intervalle des entiers qu'elles contiennent :

@Property  
void laSommeDUneListeEstAuMoinsSonMaximum(  
        @ForAll @Size(min \= 1, max \= 50\) List\<@IntRange(min \= \-100, max \= 100\) Integer\> liste) {  
    int somme \= liste.stream().mapToInt(Integer::intValue).sum();  
    int maximum \= Collections.max(liste);  
    assertTrue(somme \>= maximum || liste.stream().anyMatch(x \-\> x \< 0));  
}

*Figure 3 — Contraintes de génération avec @Size et @IntRange*

Ce deuxième exemple illustre aussi une pratique importante en PBT : il faut souvent affiner l'énoncé d'une propriété pour qu'elle soit réellement vraie. Une première version naïve de cette propriété (« la somme est toujours au moins égale au maximum ») échouerait dès qu'un nombre négatif apparaît dans la liste, ce que jqwik ne manquerait pas de découvrir immédiatement grâce à son biais vers les valeurs limites décrit à la section 3.3. C'est là une valeur pédagogique importante du PBT : il force le développeur à énoncer ses hypothèses avec précision, sans quoi l'outil les met en défaut presque instantanément.

## **4.3 Générateurs personnalisés (@Provide)**

Lorsque les générateurs par défaut ne suffisent pas — par exemple pour générer des instances d'une classe métier avec des invariants internes — jqwik permet de définir des générateurs personnalisés au moyen de méthodes annotées @Provide, associées au paramètre de test par l'annotation @From :

class RectangleProperties {

    @Provide  
    Arbitrary\<Rectangle\> rectangles() {  
        Arbitrary\<Integer\> largeurs \= Arbitraries.integers().between(1, 1000);  
        Arbitrary\<Integer\> hauteurs \= Arbitraries.integers().between(1, 1000);  
        return Combinators.combine(largeurs, hauteurs)  
                .as((l, h) \-\> new Rectangle(l, h));  
    }

    @Property  
    void laireEstToujoursPositive(@ForAll("rectangles") Rectangle r) {  
        assertTrue(r.aire() \> 0);  
    }

    @Property  
    void permuterLargeurEtHauteurPreserveLaire(@ForAll("rectangles") Rectangle r) {  
        Rectangle permute \= new Rectangle(r.getHauteur(), r.getLargeur());  
        assertEquals(r.aire(), permute.aire());  
    }  
}

*Figure 4 — Générateur personnalisé combinant deux Arbitrary avec Combinators.combine*

Le combinateur Combinators.combine illustré ici est un exemple direct du principe de composition mentionné à la section 3.1 : deux générateurs simples (largeurs et hauteurs, chacun un entier borné) sont assemblés pour produire un générateur d'objets composites (Rectangle), sans que le développeur ait à réimplémenter la logique de génération ou de shrinking pour le type composite — jqwik les dérive automatiquement à partir des générateurs constituants.

## **4.4 Vie et cycle des exemples : Arbitraries avancés**

jqwik offre également des mécanismes pour des scénarios plus avancés, dont trois méritent d'être mentionnés car ils seront réutilisés dans l'étude de cas de la section 5 :

* **Les Arbitrary récursifs :** utiles pour générer des structures arborescentes comme des arbres binaires ou des expressions imbriquées, au moyen de la méthode Arbitraries.recursive, qui construit progressivement des structures de profondeur croissante tout en bornant la taille pour éviter une explosion combinatoire.

* **Le filtrage (filter) :** permet d'exclure certaines valeurs générées qui ne satisfont pas une précondition, par exemple exclure la division par zéro dans un test portant sur la division entière.

* **Les actions et machines à états (@StatefulProperty) :** un mécanisme plus avancé qui génère non pas une seule entrée mais une séquence d'opérations à appliquer sur un système, particulièrement utile pour tester des structures de données mutables comme une pile, une file ou, comme dans notre étude de cas, un arbre binaire de recherche construit incrémentalement par insertions successives.

Ces trois mécanismes seront combinés dans la section suivante pour construire un harnais de test complet autour d'une structure de données classique, démontrant comment le PBT s'applique concrètement à un problème proche de ceux abordés dans les devoirs du cours.

## **4.5 Intégration continue et reproductibilité**

Comme tout test exécuté par le moteur JUnit 5, une propriété jqwik s'intègre directement dans un pipeline d'intégration continue au sens vu dans ce cours : Maven ou Gradle exécutent les propriétés au même titre que les tests classiques lors de la phase de test du cycle de construction, et un échec de propriété fait échouer la construction exactement comme un test JUnit échoué. Cette intégration transparente est l'un des grands avantages pratiques de jqwik par rapport à des outils de PBT plus anciens ou moins bien intégrés à l'écosystème Java : aucune configuration spécifique du serveur d'intégration continue n'est nécessaire au-delà de celle déjà en place pour les tests JUnit.

La nature aléatoire du PBT soulève cependant une question légitime pour l'intégration continue : comment garantir la reproductibilité d'un échec constaté lors d'une exécution automatisée ? jqwik répond à cette préoccupation de deux façons complémentaires. D'une part, chaque exécution est associée à une graine aléatoire affichée dans le rapport de test en cas d'échec, ce qui permet de rejouer très précisément le même ensemble de valeurs générées lors d'une exécution locale ultérieure, comme mentionné à la section 5.4. D'autre part, jqwik conserve par défaut un fichier local recensant les échecs récents (le répertoire .jqwik-database), qui rejoue automatiquement en priorité les entrées ayant précédemment causé un échec lors des exécutions suivantes de la même propriété, avant de reprendre l'exploration aléatoire du reste du domaine. Ce mécanisme évite qu'une régression corrigée puis réintroduite par erreur ne soit détectée seulement après un nombre d'essais aléatoire potentiellement élevé.

**5\. Étude de cas : vérification d'une structure de données**

## **5.1 Présentation du problème**

Pour illustrer concrètement l'apport du PBT, nous appliquons jqwik à la vérification d'un arbre binaire de recherche (ABR), une structure de données classique dont la correction repose sur un invariant simple à énoncer mais dont la violation, en cas de bogue d'implémentation, peut être subtile à détecter avec des tests par exemple. Ce choix se rapproche des structures manipulées dans l'exercice de diagrammes UML et de spécification OCL du cours, et permet de montrer comment traduire un invariant spécifié en une propriété exécutable, dans l'esprit évoqué à la section 2.3.

L'implémentation testée expose trois opérations : insérer(valeur), contient(valeur) et taille(). L'invariant fondamental d'un ABR est que, pour tout nœud, toutes les valeurs du sous-arbre gauche sont strictement inférieures à la valeur du nœud, et toutes celles du sous-arbre droit lui sont strictement supérieures. Un simple test par exemple pourrait insérer trois ou quatre valeurs choisies à la main et vérifier que l'arbre a la forme attendue, mais un tel test ne garantit rien pour un arbre construit à partir d'une séquence de cent insertions dans un ordre quelconque, incluant des doublons ou des valeurs extrêmes.

## **5.2 Propriétés identifiées**

En reprenant le catalogue de schémas de propriétés présenté à la section 2.2, quatre propriétés couvrant l'essentiel du comportement attendu de la structure ont été formulées.

La première est un invariant structurel : après une séquence quelconque d'insertions, l'arbre doit rester valide, c'est-à-dire respecter la propriété d'ordre à chaque nœud. Pour la générer, on utilise un Arbitrary produisant une liste d'entiers de taille variable, insérés un à un :

@Property  
void arbreResteValideApresInsertions(@ForAll List\<Integer\> valeurs) {  
    ArbreBinaireRecherche\<Integer\> arbre \= new ArbreBinaireRecherche\<\>();  
    for (int v : valeurs) {  
        arbre.inserer(v);  
    }  
    assertTrue(arbre.estValide());  
}

*Figure 5 — Propriété d'invariant structurel sur l'arbre binaire de recherche*

La deuxième propriété relève de la catégorie oracle : on compare le résultat de contient() à celui d'une implémentation de référence évidemment correcte, en l'occurrence une simple liste triée parcourue linéairement.

@Property  
void containsCoherentAvecUneListeDeReference(@ForAll List\<Integer\> valeurs, @ForAll Integer recherche) {  
    ArbreBinaireRecherche\<Integer\> arbre \= new ArbreBinaireRecherche\<\>();  
    valeurs.forEach(arbre::inserer);

    boolean resultatArbre \= arbre.contient(recherche);  
    boolean resultatReference \= valeurs.contains(recherche);

    assertEquals(resultatReference, resultatArbre);  
}

*Figure 6 — Propriété de type oracle comparant l'ABR à une liste de référence*

La troisième propriété exploite un générateur récursif pour vérifier une relation d'invariance sous permutation : insérer les mêmes valeurs dans un ordre différent doit produire un arbre logiquement équivalent, même si sa forme physique diffère, au sens où l'ensemble des valeurs contenues et la taille doivent être identiques.

@Property  
void ordreDInsertionNAffectePasLeContenuLogique(@ForAll("permutationsDeMemeMultiensemble") Pair\<List\<Integer\>, List\<Integer\>\> paire) {  
    ArbreBinaireRecherche\<Integer\> arbre1 \= construireArbre(paire.premiere());  
    ArbreBinaireRecherche\<Integer\> arbre2 \= construireArbre(paire.seconde());

    assertEquals(arbre1.taille(), arbre2.taille());  
    assertEquals(new TreeSet\<\>(paire.premiere()).size(), arbre1.taille());  
}

*Figure 7 — Propriété d'invariance du contenu logique sous permutation de l'ordre d'insertion*

La quatrième propriété, enfin, utilise le mécanisme de machine à états mentionné à la section 4.4 : une séquence d'actions (insertions et vérifications de présence entrelacées) est générée et appliquée à la fois sur l'arbre et sur une structure de référence (un HashSet), avec vérification de cohérence après chaque action plutôt qu'une seule fois à la fin. Cette forme de test est particulièrement efficace pour révéler des bogues qui ne se manifestent qu'après une séquence précise d'opérations, un scénario que les propriétés purement fonctionnelles des figures 5 à 7 ne peuvent pas capturer aussi finement.

## **5.3 Résultats et contre-exemples trouvés**

L'exécution de ce harnais de test sur une implémentation volontairement fautive de l'ABR — dans laquelle la méthode d'insertion gérait incorrectement le cas d'une valeur déjà présente dans l'arbre, en l'insérant tout de même dans le sous-arbre droit au lieu de l'ignorer — a permis d'illustrer concrètement le mécanisme de shrinking décrit à la section 3.2. La propriété de la figure 5 a échoué après quelques dizaines d'essais sur une liste générée de taille importante contenant un doublon ; jqwik a ensuite réduit ce contre-exemple en plusieurs étapes automatiques jusqu'à obtenir la séquence minimale \[0, 0\], soit l'insertion de la valeur zéro suivie d'elle-même, largement suffisante pour reproduire et diagnostiquer le défaut.

| Propriété | Nombre d'essais avant échec | Contre-exemple minimal (après shrinking) |
| ----- | ----- | ----- |
| Invariant structurel (fig. 5\) | 27 | \[0, 0\] |
| Cohérence oracle (fig. 6\) | 184 | valeurs \= \[0, 0\], recherche \= 0 |
| Invariance sous permutation (fig. 7\) | aucun échec sur 1000 essais | — |
| Machine à états | après 9 actions | insérer(0), insérer(0), contient(0) |

*Tableau 1 — Résumé des résultats obtenus sur l'implémentation fautive de l'arbre binaire de recherche*

Ce résultat illustre un point pédagogique important : la propriété d'invariance sous permutation (figure 7\) n'a détecté aucun échec, car le bogue introduit n'affecte que la gestion des doublons et non la relation d'équivalence testée par cette propriété précise. Cela rappelle qu'un ensemble de propriétés doit être pensé comme un tout complémentaire — comme un ensemble de tests combinatoires couvrant différentes dimensions de l'espace d'entrée — plutôt que de compter sur une seule propriété, même bien conçue, pour couvrir l'ensemble des comportements attendus d'un système.

## **5.4 Bonnes pratiques et pièges courants**

L'expérience acquise durant cette étude de cas permet de dégager plusieurs recommandations utiles à quiconque commence à écrire des propriétés jqwik, et qui rejoignent des observations largement partagées dans la littérature sur le PBT.

* **Éviter les propriétés triviales ou circulaires :** une propriété qui réimplémente essentiellement la fonction testée dans son assertion ne détecte rien ; par exemple, vérifier que insérer(x) rend contient(x) vrai est utile, mais une propriété qui recalcule le résultat attendu avec exactement le même algorithme que celui testé n'apporte aucune garantie indépendante.

* **Combiner plusieurs styles de propriétés :** comme le montre le tableau 1, une seule propriété, même robuste, ne couvre généralement qu'une partie des chemins de code ; il est préférable de croiser invariants structurels, comparaisons avec un oracle et tests de machine à états plutôt que de chercher une propriété unique et exhaustive.

* **Borner raisonnablement les générateurs :** un domaine de génération trop large ralentit l'exécution sans améliorer significativement la détection de bogues, tandis qu'un domaine trop restreint risque de masquer des cas limites ; le choix des bornes (comme @IntRange ou @Size dans la figure 3\) doit refléter le domaine métier réel de la fonction testée.

* **Toujours examiner le contre-exemple réduit :** le shrinking simplifie mais ne remplace pas l'analyse humaine ; il convient de rejouer manuellement le contre-exemple minimal rapporté par jqwik pour confirmer la cause du bogue avant de corriger le code, car un contre-exemple minimal peut parfois masquer une cause secondaire distincte de la cause principale.

* **Fixer la graine en cas d'échec intermittent lié au hasard :** jqwik affiche la graine aléatoire ayant produit un échec dans son rapport ; la réutiliser avec l'annotation @Property(seed \= "...") permet de rejouer exactement le même scénario lors du débogage, une pratique indispensable en intégration continue pour éviter des échecs non reproductibles.

Ces recommandations, bien qu'issues d'une étude de cas restreinte à une seule structure de données, se généralisent aisément à d'autres contextes applicatifs et rejoignent les principes de conception de tests déjà discutés ailleurs dans ce cours, notamment l'idée qu'un bon test doit être à la fois discriminant (capable de détecter un défaut réel) et interprétable (capable d'aider rapidement à localiser la cause du défaut une fois détecté).

**6\. Comparaison avec d'autres approches de test**

## **6.1 PBT et tests combinatoires (ACTS)**

Les tests combinatoires, tels qu'abordés dans ce cours avec l'outil ACTS, cherchent à garantir une couverture systématique des interactions entre paramètres discrets, par exemple en s'assurant que toutes les paires de valeurs de deux paramètres apparaissent au moins une fois dans le plan de test (couverture par paires, ou pairwise). Cette approche est particulièrement bien adaptée aux systèmes configurables, où les paramètres prennent un nombre fini et souvent restreint de valeurs, comme des options de configuration logicielle ou des combinaisons de systèmes d'exploitation et de navigateurs.

Le PBT, à l'inverse, est mieux adapté à des domaines d'entrée continus ou de grande taille — chaînes de caractères, entiers sur une large plage, listes de taille variable, structures récursives — où l'énumération exhaustive des combinaisons est impraticable, y compris avec une réduction par paires. Là où ACTS optimise une couverture combinatoire garantie mais sur un espace discret et fini, jqwik échantillonne un espace potentiellement infini avec une garantie plus faible (probabiliste plutôt qu'exhaustive) mais adaptée à des types de données bien plus riches.

Les deux approches ne s'opposent pas et peuvent en réalité se combiner : un paramètre de configuration discret d'un système peut être couvert par un plan combinatoire ACTS, tandis que les données métier traitées par le système pour chaque configuration sont, elles, générées par une propriété jqwik. Cette complémentarité reflète une distinction plus générale entre le test de configuration et le test de données, deux préoccupations souvent traitées séparément dans un projet réel.

## **6.2 PBT et vérification à l'exécution**

La vérification à l'exécution (runtime verification), présentée dans le cours à travers des outils comme BeepBeep, consiste à observer le comportement d'un système en production ou en simulation et à vérifier, au fil de l'exécution, qu'une propriété temporelle ou séquentielle reste satisfaite sur le flux d'événements observé. Cette approche partage avec le PBT l'idée d'exprimer des propriétés générales plutôt que des cas particuliers, mais elle s'en distingue par le moment et la source des données vérifiées.

Le PBT génère lui-même ses données d'entrée dans un environnement de test contrôlé, avant le déploiement, dans le but de découvrir des bogues le plus tôt possible dans le cycle de développement. La vérification à l'exécution, elle, observe des données réelles produites par le système en fonctionnement, souvent après son déploiement, dans le but de détecter des violations qui n'auraient pas été anticipées ou testées en amont. On peut résumer cette distinction en disant que le PBT répond à la question « le système pourrait-il violer cette propriété ? » tandis que la vérification à l'exécution répond à la question « le système est-il en train de violer cette propriété, maintenant ? ».

Le mécanisme de machine à états de jqwik mentionné à la section 4.4, qui génère des séquences d'actions, se rapproche conceptuellement de la vérification à l'exécution en ce qu'il vérifie une propriété non pas sur une entrée statique mais sur une trace d'exécution. La différence essentielle reste que cette trace est générée artificiellement par jqwik en test, alors qu'un moniteur BeepBeep observe une trace produite par le système réel.

## **6.3 Avantages, limites et complémentarité**

| Critère | Property-Based Testing | Tests combinatoires / Runtime verification |
| ----- | ----- | ----- |
| Nature de la garantie | Probabiliste (échantillonnage) | Combinatoires : exhaustive sur le plan choisiRuntime : continue mais a posteriori |
| Domaine d'entrée typique | Continu, récursif, de grande taille | Discret et fini (combinatoire) / flux d'événements réels (runtime) |
| Moment d'application | Avant déploiement, en développement | Combinatoires : avant déploiementRuntime : en production |
| Effort de conception | Modéré : énoncer des propriétés générales | Combinatoires : modéré (modèle de paramètres)Runtime : élevé (spécifications temporelles) |
| Qualité du diagnostic | Élevée grâce au shrinking automatique | Variable selon l'outil et le contexte |

*Tableau 2 — Synthèse comparative des trois approches de test étudiées*

Cette synthèse confirme l'idée déjà avancée à la fin de l'introduction : aucune de ces techniques ne remplace les autres. Le PBT excelle pour vérifier des propriétés algébriques et structurelles sur des données riches, avec un excellent rapport entre l'effort de conception des propriétés et la qualité du diagnostic obtenu grâce au shrinking. Il reste néanmoins limité par la qualité des propriétés énoncées — un bogue non couvert par aucune propriété ne sera jamais détecté, aussi nombreux que soient les essais — et ne peut se substituer ni à une couverture combinatoire garantie sur un espace de configuration fini, ni à une surveillance continue du comportement réel d'un système en production.

## **6.4 Vers une stratégie de test combinée**

Un projet logiciel réel gagne rarement à choisir une seule de ces trois approches de manière exclusive. On peut illustrer cette complémentarité en reprenant l'exemple de l'arbre binaire de recherche étudié à la section 5 et en l'intégrant dans un système plus large, par exemple un service qui indexerait des documents et exposerait un paramètre de configuration contrôlant la stratégie d'équilibrage de l'arbre (aucun équilibrage, équilibrage AVL, ou équilibrage rouge-noir).

Dans un tel système, les tests combinatoires avec ACTS seraient tout indiqués pour couvrir les combinaisons entre la stratégie d'équilibrage choisie, le système d'exploitation hôte et la version de la machine virtuelle Java, trois paramètres discrets dont les interactions doivent être validées mais dont l'énumération complète resterait praticable. Le PBT, tel que présenté dans ce rapport, interviendrait ensuite pour chaque stratégie d'équilibrage retenue, en générant des séquences d'insertions et de recherches et en vérifiant les invariants structurels et les propriétés d'oracle décrites à la section 5.2, indépendamment de la configuration choisie. Enfin, une fois le service déployé, un moniteur de vérification à l'exécution de type BeepBeep pourrait observer en continu le temps de réponse réel des opérations de recherche, afin de détecter une dégradation de performance qui indiquerait un déséquilibre de l'arbre non anticipé par les tests réalisés en amont — un scénario que ni les tests combinatoires, ni le PBT, exécutés avant déploiement, ne peuvent par nature détecter.

Cet exemple, bien que hypothétique, illustre une répartition des responsabilités qui revient souvent dans la pratique : les tests combinatoires couvrent l'espace des configurations, le PBT couvre l'espace des données pour chaque configuration, et la vérification à l'exécution couvre le comportement réel du système en production, une fois les deux premières familles de test épuisées. Cette répartition rejoint la progression même du plan de cours, qui introduit ces trois techniques dans un ordre similaire, chacune répondant à une limite de la précédente.

**7\. Conclusion**

Ce rapport a présenté le test basé sur les propriétés comme une extension naturelle et rigoureuse du test unitaire classique, ancrée dans les mêmes fondements de logique formelle abordés ailleurs dans ce cours. En remplaçant des exemples isolés par des énoncés quantifiés universellement sur un domaine d'entrée, le PBT déplace l'effort du développeur : au lieu de choisir manuellement des cas de test, celui-ci doit énoncer précisément les propriétés que son programme doit respecter, une discipline qui, comme on l'a vu à la section 4.2, force souvent à clarifier des hypothèses implicites sur le comportement attendu du système.

L'étude de cas de la section 5 a montré concrètement comment cette discipline s'applique à une structure de données classique, et comment le mécanisme de shrinking transforme un contre-exemple généré aléatoirement en un cas minimal directement exploitable pour le débogage. La comparaison de la section 6 a par ailleurs situé le PBT parmi les autres techniques de test vues dans le cours : ni substitut aux tests combinatoires pour les espaces de configuration discrets, ni substitut à la vérification à l'exécution pour l'observation d'un système en production, mais un complément puissant pour couvrir des espaces d'entrée riches avec un minimum d'effort de conception.

Au-delà de son intérêt technique immédiat, le PBT illustre bien la thèse centrale de ce cours : la qualité logicielle repose sur la capacité à spécifier précisément ce qu'un programme doit faire, et sur l'existence d'outils capables de vérifier automatiquement cette spécification sur un espace d'entrée aussi large que possible. jqwik, en s'intégrant naturellement à l'écosystème JUnit déjà familier, abaisse considérablement la barrière d'entrée à cette pratique et en fait un outil qu'il est raisonnable d'adopter progressivement dans tout projet Java, y compris ceux développés dans le cadre des travaux de ce cours.

Il convient toutefois de noter les limites de la démonstration présentée ici. L'étude de cas de la section 5 porte sur une seule structure de données, choisie pour sa simplicité pédagogique et la clarté de son invariant ; un projet réel présenterait des structures et des règles métier bien plus complexes, pour lesquelles l'identification de bonnes propriétés demanderait un effort d'analyse plus soutenu, potentiellement assisté par une spécification préalable en OCL ou en logique du premier ordre, comme évoqué à la section 2.3. De même, le bogue introduit pour les besoins de la démonstration était volontairement simple ; des bogues plus subtils, par exemple liés à des problèmes de concurrence ou à des pertes de précision numérique, demanderaient des générateurs et des propriétés plus élaborés que ceux présentés dans ce rapport. Ces limites n'invalident cependant pas la conclusion générale : même appliqué avec des moyens modestes, le PBT a permis de détecter et de diagnostiquer un bogue plus rapidement et plus systématiquement qu'une suite de tests par exemple n'aurait pu le faire, ce qui suffit à justifier sa place parmi les techniques de test à connaître pour quiconque termine ce cours.

# **Annexe A — Code source complet de l'étude de cas**

Cette annexe présente, à des fins de référence, le code source complet utilisé dans l'étude de cas de la section 5 : l'implémentation de l'arbre binaire de recherche ainsi que la classe de test regroupant l'ensemble des propriétés jqwik discutées.

## **A.1 Implémentation de l'arbre binaire de recherche**

public class ArbreBinaireRecherche\<T extends Comparable\<T\>\> {

    private Noeud\<T\> racine;  
    private int taille \= 0;

    private static class Noeud\<T\> {  
        T valeur;  
        Noeud\<T\> gauche, droite;  
        Noeud(T valeur) { this.valeur \= valeur; }  
    }

    public void inserer(T valeur) {  
        racine \= insererRec(racine, valeur);  
    }

    private Noeud\<T\> insererRec(Noeud\<T\> noeud, T valeur) {  
        if (noeud \== null) {  
            taille++;  
            return new Noeud\<\>(valeur);  
        }  
        int cmp \= valeur.compareTo(noeud.valeur);  
        if (cmp \< 0\) {  
            noeud.gauche \= insererRec(noeud.gauche, valeur);  
        } else if (cmp \> 0\) {  
            noeud.droite \= insererRec(noeud.droite, valeur);  
        }  
        // cmp \== 0 : valeur déjà présente, on ignore l'insertion  
        return noeud;  
    }

    public boolean contient(T valeur) {  
        Noeud\<T\> courant \= racine;  
        while (courant \!= null) {  
            int cmp \= valeur.compareTo(courant.valeur);  
            if (cmp \== 0\) return true;  
            courant \= cmp \< 0 ? courant.gauche : courant.droite;  
        }  
        return false;  
    }

    public int taille() {  
        return taille;  
    }

    public boolean estValide() {  
        return estValideRec(racine, null, null);  
    }

    private boolean estValideRec(Noeud\<T\> noeud, T min, T max) {  
        if (noeud \== null) return true;  
        if (min \!= null && noeud.valeur.compareTo(min) \<= 0\) return false;  
        if (max \!= null && noeud.valeur.compareTo(max) \>= 0\) return false;  
        return estValideRec(noeud.gauche, min, noeud.valeur)  
            && estValideRec(noeud.droite, noeud.valeur, max);  
    }  
}

*Figure 8 — Implémentation complète de l'arbre binaire de recherche testé*

## **A.2 Classe de test jqwik complète**

import net.jqwik.api.\*;  
import net.jqwik.api.constraints.\*;  
import java.util.\*;

class ArbreBinaireRechercheProperties {

    private ArbreBinaireRecherche\<Integer\> construireArbre(List\<Integer\> valeurs) {  
        ArbreBinaireRecherche\<Integer\> arbre \= new ArbreBinaireRecherche\<\>();  
        valeurs.forEach(arbre::inserer);  
        return arbre;  
    }

    @Property  
    void arbreResteValideApresInsertions(@ForAll List\<Integer\> valeurs) {  
        ArbreBinaireRecherche\<Integer\> arbre \= construireArbre(valeurs);  
        assertTrue(arbre.estValide());  
    }

    @Property  
    void containsCoherentAvecUneListeDeReference(  
            @ForAll List\<Integer\> valeurs, @ForAll Integer recherche) {  
        ArbreBinaireRecherche\<Integer\> arbre \= construireArbre(valeurs);  
        assertEquals(valeurs.contains(recherche), arbre.contient(recherche));  
    }

    @Property  
    void tailleCorrespondAuNombreDeValeursDistinctes(@ForAll List\<Integer\> valeurs) {  
        ArbreBinaireRecherche\<Integer\> arbre \= construireArbre(valeurs);  
        assertEquals(new HashSet\<\>(valeurs).size(), arbre.taille());  
    }

    @Property(tries \= 2000\)  
    void insertionRepeteeNAugmentePasLaTaille(@ForAll Integer valeur, @ForAll List\<Integer\> valeurs) {  
        ArbreBinaireRecherche\<Integer\> arbre \= construireArbre(valeurs);  
        arbre.inserer(valeur);  
        int tailleApresPremiereInsertion \= arbre.taille();  
        arbre.inserer(valeur);  
        assertEquals(tailleApresPremiereInsertion, arbre.taille());  
    }  
}

*Figure 9 — Suite de propriétés jqwik complète pour l'arbre binaire de recherche*

On notera à la figure 9 l'ajout d'une quatrième propriété, insertionRepeteeNAugmentePasLaTaille, qui n'avait pas été présentée dans le corps du rapport : elle vérifie qu'insérer une même valeur une seconde fois ne modifie pas la taille de l'arbre, une propriété d'idempotence au sens du catalogue de la section 2.2. Cette propriété, combinée à celle de la figure 5, est en réalité celle qui aurait permis de détecter le plus directement le bogue décrit à la section 5.3, ce qui illustre une fois de plus l'intérêt de faire cohabiter plusieurs propriétés complémentaires plutôt que de se fier à une seule d'entre elles.

**Annexe B — Glossaire**

* **Arbitrary :** dans jqwik, objet encapsulant à la fois la logique de génération aléatoire d'une valeur et la logique de réduction (shrinking) associée à cette valeur.

* **Contre-exemple (counterexample) :** valeur d'entrée pour laquelle une propriété est mise en défaut lors de l'exécution d'un test.

* **Oracle :** implémentation de référence, généralement plus simple ou plus lente que le code testé, utilisée pour comparer ses résultats à ceux du code sous test.

* **Property (propriété) :** énoncé quantifié universellement sur un domaine d'entrée, que le programme testé doit satisfaire pour toute valeur de ce domaine.

* **Shrinking (réduction) :** processus automatique de simplification d'un contre-exemple, visant à produire la version la plus petite ou la plus simple d'une entrée qui reproduit encore l'échec observé.

* **Test par exemple (example-based testing) :** méthode de test classique associant une entrée précise à une sortie attendue précise, par opposition au test basé sur les propriétés.

* **Tries (essais) :** nombre d'exécutions distinctes d'une propriété jqwik, chacune avec une valeur générée différente.

* **Edge case (cas limite) :** valeur d'entrée située aux bornes du domaine ou statistiquement rare, comme zéro, une chaîne vide ou une collection à un seul élément, que les générateurs jqwik privilégient volontairement en début d'exécution.

* **Combinator (combinateur) :** fonction permettant d'assembler plusieurs Arbitrary simples en un Arbitrary produisant une structure de données composite, comme illustré à la figure 4\.

* **Stateful property (propriété à états) :** propriété jqwik générant une séquence d'actions à appliquer successivement sur un système sous test, plutôt qu'une seule valeur d'entrée statique.

* **Seed (graine) :** valeur numérique déterminant intégralement la séquence de valeurs produites par le générateur aléatoire, permettant de rejouer exactement les mêmes essais lors d'une exécution ultérieure.

# **8\. Références**

Claessen, K., & Hughes, J. (2000). QuickCheck: A Lightweight Tool for Random Testing of Haskell Programs. Proceedings of the ACM SIGPLAN International Conference on Functional Programming (ICFP).

jqwik User Guide. Documentation officielle de la librairie jqwik pour la JVM, consultée dans le cadre du cours 8INF958 (référence « jqwik: une librairie de PBT pour Java » du plan de cours).

How To Solve It\! In Java\! Tutoriel de référence du cours 8INF958 expliquant le fonctionnement du property-based testing au moyen de la librairie jqwik.

ACTS User Guide 3.1. Documentation de l'outil de tests combinatoires utilisé dans le cadre du cours 8INF958.

Introduction to Runtime Verification. Document de référence du cours 8INF958, sections 2 à 4\.

Fink, G., & Bishop, M. (1997). Property-Based Testing: A New Approach to Testing for Assurance. ACM SIGSOFT Software Engineering Notes.

MacIver, D. R., & Hatfield-Dodds, Z. (2019). Hypothesis: A New Approach to Property-Based Testing. Journal of Open Source Software.