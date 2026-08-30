# Overwatch Tracker

Application web personnelle pour consulter et historiser les statistiques publiques d’un joueur Overwatch. Le frontend ne contacte jamais OverFast directement : toutes les requêtes passent par le backend Spring Boot.

## Stack

- Angular 22, TypeScript et Angular Material ;
- Spring Boot 3.5 / Java 21, Spring Data JPA, Caffeine et springdoc-openapi ;
- PostgreSQL 17 ;
- Docker Compose et nginx.

Spring Boot a été conservé : l’intégration REST, le cache, la validation, la persistance et Swagger sont bien couverts et ne justifient pas un changement vers FastAPI.

## Démarrage avec Docker

Prérequis : Docker avec le plugin Compose.

```bash
cp .env.example .env
docker compose up --build
```

URLs locales :

- application : http://localhost:4200
- API : http://localhost:8080/api
- Swagger UI : http://localhost:8080/swagger-ui.html
- spécification OpenAPI : http://localhost:8080/v3/api-docs
- santé du backend : http://localhost:8080/actuator/health
- santé du frontend : http://localhost:4200/healthz

## Développement local

Le backend nécessite Java 21 et un PostgreSQL disponible. Maven n'a pas besoin
d'être installé : le wrapper inclus garantit la bonne version.

```bash
cd backend
./mvnw spring-boot:run
```

Le frontend Angular 22 nécessite Node 24.15 ou une version LTS plus récente :

```bash
cd frontend
npm ci
npm start
```

Le serveur Angular utilise déjà `proxy.conf.json` pour transmettre `/api` à `localhost:8080`. Les variables de connexion peuvent être surchargées à partir de `.env.example`.

## API du projet

- `POST /api/players/lookup` — importe un BattleTag public ;
- `POST /api/players/{BattleTag}/refresh` — invalide le cache et réimporte ;
- `GET /api/players/{BattleTag}` — recharge le dernier profil mémorisé ;
- `GET /api/players/{BattleTag}/history` — retourne jusqu’à 100 snapshots chronologiques.
- `GET /api/players/recent` — liste les profils déjà consultés ;
- `GET /api/heroes` — fournit les métadonnées et portraits des héros.

Le format accepté dans la recherche est `Pseudo#1234`. Les routes du navigateur
utilisent la forme encodée, par exemple `/profil/Pseudo-1234` et
`/historique/Pseudo-1234`.

## Structure

```text
frontend/src/app/
├── core/               # client HTTP, store et modèles partagés
├── pages/              # accueil, profil, historique, aide
└── app.routes.ts       # routes pilotées par le BattleTag
backend/src/main/java/fr/overwatchtracker/
├── api/                # contrôleurs et gestion uniforme des erreurs
├── config/             # client HTTP
├── domain/             # entité et repository de snapshots
├── dto/                # contrats exposés
├── integration/        # passerelle et client OverFast dédiés
└── service/            # orchestration, mapping et mode démo
```

Flyway crée puis fait évoluer la table `player_snapshots`. Chaque import conserve
les indicateurs des graphiques ainsi que le DTO complet en JSONB versionné. La
migration V2 convertit les anciennes données sans supprimer les snapshots.

## Cache, erreurs et tests

Le client OverFast est mis en cache côté backend avec Caffeine (durée configurable par `OVERFAST_CACHE_MINUTES`; le MVP utilise le cache Spring par défaut dans le processus). Le bouton Actualiser invalide explicitement l’entrée. Le backend traduit les statuts OverFast 404, 429, 503 et 504 en erreurs métier françaises. Les tests unitaires de `OverfastClient` utilisent un faux serveur HTTP :

```bash
cd backend
./mvnw test

cd ../frontend
npm ci
npm test
npm run build
```

## Exploitation et remise à zéro

Pour arrêter l'application en conservant les profils :

```bash
docker compose down
```

Pour repartir volontairement avec une base vide :

```bash
docker compose down --volumes
```

Attention : la seconde commande supprime définitivement tous les snapshots du
volume PostgreSQL. Elle n'est pas nécessaire après une mise à jour normale : les
migrations Flyway préservent les données existantes.

## Limites et confidentialité

[OverFast API](https://github.com/TeKrop/overfast-api) est une API communautaire non officielle qui extrait les pages de carrière Blizzard publiques. Au moment de l’implémentation, l’instance publique expose notamment `GET /players/{player_id}` et applique ses propres caches et limites de débit. Elle peut changer, être indisponible ou retourner des données incomplètes. Les profils privés ne sont pas accessibles et certains rangs/statistiques dépendent de ce que Blizzard publie.

L’application ne demande et ne stocke **jamais** de mot de passe Battle.net. Elle conserve seulement le BattleTag et les statistiques publiques importées. Elle n’est affiliée ni à Blizzard Entertainment ni au projet OverFast.
