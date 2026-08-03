# Argos

Argos est un outil de streaming conçu pour détecter automatiquement des **antipatterns de collaboration** à partir des journaux (logs) de serveurs Perforce, dans le contexte du développement de jeux vidéo.

## Contexte

Les serveurs de gestion de versions enregistrent silencieusement chaque action d'une équipe de développement, mais ces journaux sont rarement exploités pour détecter des comportements collaboratifs contre-productifs. Dans l'industrie du jeu vidéo, Perforce gère une activité particulièrement dense sur des **assets binaires non fusionnables**, rendant les analyses classiques par diff textuel inopérantes. Une approche fondée sur les **séquences d'événements** est donc nécessaire.

## Antipatterns détectés

Parmi les 168 antipatterns catalogués dans **ReliSA**, Argos en opérationnalise 5 :

- **Collective Procrastination**
- **Warm Bodies**
- **Lone Wolf**
- **Bystander Apathy**
- Un proxy pour **Cascading Branches**

## Architecture

Argos repose sur deux phases complémentaires :

1. **Phase métriques** : reproduction hors ligne des 4 instruments OpenMetrics popularisés par Prometheus (*Counter*, *Gauge*, *Histogram*, *Summary*) pour caractériser l'activité globale du dépôt.
2. **Phase détection par CEP** : intégration du *complex event processing* via **BeepBeep 3.13**, permettant de détecter des motifs sensibles à l'ordre des événements (ex. un fichier modifié mais jamais soumis). La détection repose sur des automates à états finis par fichier, suivant les transitions *edit*, *submit* et *revert* sans nécessiter le chargement complet du log en mémoire.

## Résultats

Évalué sur un log de **1,7 Go** issu d'un cours de développement de jeu multi-équipes :

- **16 262** événements uniques extraits en environ **14 secondes**
- **217** séquences Edit-then-Revert identifiées
- **250** fichiers abandonnés détectés
- Validation croisée interne : la valeur terminale de la jauge *open-files* correspond exactement au nombre d'alertes d'apathie (**250** dans les deux cas)

## Installation

```
git clone https://github.com/hanifahalphaboda13-stack/Argos.git
cd Argos
```

## Données

⚠️ Les logs Perforce utilisés pour l'évaluation proviennent d'un cours universitaire et sont **confidentiels**. Ils ne sont donc pas inclus dans ce dépôt.


