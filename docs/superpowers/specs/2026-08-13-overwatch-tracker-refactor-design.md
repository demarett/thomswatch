# Overwatch Tracker — Refonte technique

## Objectif

Refondre l'application personnelle Overwatch Tracker afin de corriger les défauts de validation, de persistance, de transaction, de navigation, de tests et de déploiement identifiés pendant la revue, tout en conservant les fonctionnalités visibles du MVP.

L'application reste destinée à un usage personnel. L'authentification, le rate limiting distribué et l'exploitation publique ne font pas partie de cette refonte. Les données PostgreSQL existantes peuvent être supprimées et recréées.

## Architecture backend

Le backend conserve Spring Boot 3.5 et Java 21, avec quatre frontières explicites :

- `OverfastGateway` possède exclusivement le client HTTP OverFast, les délais de connexion et de lecture, le cache et la traduction des erreurs amont.
- `PlayerProfileMapper` transforme une réponse OverFast en modèle applicatif sans accès réseau ni accès à la base.
- `PlayerApplicationService` orchestre la normalisation du BattleTag, le mode démonstration, le chargement ou l'actualisation d'un profil et la lecture des vues enregistrées. Cette orchestration ne porte pas de transaction.
- `SnapshotService` possède les transactions courtes de persistance et délègue les requêtes SQL au repository.

Un appel de chargement suit ce flux : contrôleur validé → service applicatif → gateway/cache → mapper → service transactionnel de snapshot → réponse DTO. Aucun appel réseau ne s'exécute dans une transaction SQL.

Les erreurs métier utilisent des codes stables et sont traduites par un gestionnaire global. Une requête absente, vide ou mal formée retourne HTTP 400 avec le contrat `ApiError`; les indisponibilités OverFast conservent leurs statuts 404, 429, 503 ou 504 appropriés.

## Modèle de données

La migration initiale est remplacée, puisque la réinitialisation des données est autorisée. La table de snapshots contient :

- l'identité normalisée du BattleTag et les métadonnées d'affichage ;
- la date de capture ;
- les métriques nécessaires aux graphiques ;
- un payload `JSONB` ;
- une version de schéma de payload non nulle.

Les contraintes garantissent les champs obligatoires et une version positive. Un index couvre `(battle_tag, captured_at DESC)`.

Le repository expose deux lectures dédiées :

- les 100 snapshots les plus récents d'un BattleTag, sélectionnés en ordre décroissant puis remis en ordre chronologique par le service ;
- le dernier snapshot de chacun des huit BattleTags les plus récemment consultés, dédupliqués directement en SQL.

Le format de payload commence à la version `1`. Le lecteur est structuré par version afin qu'une future évolution puisse ajouter un migrateur sans coupler la persistence à la forme courante des DTO.

## Architecture frontend

Le frontend conserve Angular 22, Angular Material et les composants standalone.

Les routes canoniques deviennent :

- `/profil/:battleTag` ;
- `/historique/:battleTag`.

Le BattleTag utilisé dans une URL est normalisé sous la forme compatible `Pseudo-1234`. Les pages chargent leurs données depuis le paramètre de route ; elles fonctionnent donc après rechargement ou accès direct. Un `PlayerStore` centralise seulement l'état courant, le chargement et les erreurs, tandis que `PlayerApi` possède les appels HTTP. Le store améliore la navigation mais ne constitue jamais l'unique source de l'identité du joueur.

La recherche navigue vers la route du profil après création du snapshot. L'ouverture d'un profil récent charge le snapshot enregistré. L'actualisation remplace l'état courant et ajoute un snapshot. L'historique peut être ouvert indépendamment du profil.

## Tests

Le backend reçoit le Maven Wrapper et des tests à trois niveaux :

- tests unitaires du mapper et de la normalisation ;
- tests MVC pour les BattleTags absents, nuls, invalides et valides, ainsi que le contrat d'erreur ;
- tests de repository avec PostgreSQL/Testcontainers pour vérifier plus de 100 snapshots et la déduplication des profils récents.

Les tests du gateway vérifient les statuts OverFast, les réponses invalides, les délais et les chemins envoyés au faux serveur HTTP.

Le frontend configure le runner de tests Angular 22 avec Vitest. Les tests couvrent l'API, le store, la recherche, l'accès direct à une route de profil, l'actualisation, l'historique et les erreurs HTTP. `npm test` s'exécute une fois sans mode watch.

Le développement suit un cycle test-first : chaque défaut comportemental est d'abord reproduit par un test qui échoue pour la raison attendue, puis corrigé par le changement minimal avant refactorisation.

## Construction et exploitation locale

Le Dockerfile frontend utilise `npm ci`. Les builds multi-étapes restent reproductibles et les conteneurs applicatifs utilisent un utilisateur non privilégié compatible avec leur image.

Spring Boot Actuator expose uniquement l'état de santé nécessaire à Docker. PostgreSQL, backend et frontend possèdent chacun un health check. Docker Compose attend une base saine avant le backend, puis un backend sain avant le frontend.

Les variables de connexion et OverFast restent configurables par environnement. Les valeurs par défaut sont réservées au développement local et la documentation explique explicitement que la reconstruction du schéma nécessite la suppression volontaire du volume PostgreSQL existant.

## Hors périmètre

- authentification et gestion de comptes ;
- exposition publique ou multi-utilisateur ;
- rate limiting distribué ;
- métriques, traces distribuées et plateforme de logs ;
- conservation ou migration des snapshots existants ;
- nouvelles fonctionnalités statistiques visibles.

## Critères d'acceptation

- Les BattleTags absents, nuls ou invalides produisent une réponse HTTP 400 uniforme.
- Aucun appel HTTP OverFast ne s'exécute dans une transaction de base de données.
- Les délais de connexion et de lecture configurés sont effectivement appliqués.
- L'historique contient au maximum les 100 captures les plus récentes et les retourne chronologiquement.
- La liste récente contient au maximum huit BattleTags distincts, même si un joueur possède plus de 100 captures.
- `/profil/:battleTag` et `/historique/:battleTag` fonctionnent après un rechargement direct.
- Les payloads de snapshots portent une version explicite et sont lus par un composant versionné.
- `./mvnw test`, `npm test`, `npm run build` et `docker compose config` réussissent dans leurs versions d'exécution documentées.
- Docker Compose exprime et vérifie l'ordre de santé PostgreSQL → backend → frontend.

