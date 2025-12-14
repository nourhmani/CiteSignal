# CiteSignal - Plateforme de Gestion d'Incidents dans une Ville Intelligente

## Description

CiteSignal est une application web développée avec Spring Boot permettant aux citoyens de signaler en temps réel des problèmes urbains (trous dans la chaussée, lampadaires défectueux, déchets non collectés, etc.). Les services municipaux peuvent ensuite traiter ces signalements de manière efficace.

## Technologies utilisées

- **Backend**: Spring Boot 3.2.0
- **Sécurité**: Spring Security avec JWT
- **Base de données**: MySQL
- **Frontend**: Thymeleaf
- **Email**: Spring Mail
- **Build**: Maven

## Prérequis

- Java 17 ou supérieur
- Maven 3.6+
- MySQL 8.0+
- Un compte email pour l'envoi de notifications (Gmail, SendGrid, etc.)

## Installation

### 1. Naviguer dans le projet

```bash
cd citesignal
```

### 2. Configurer la base de données

Créer une base de données MySQL :

```sql
CREATE DATABASE citesignal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configurer l'application

Modifier le fichier `src/main/resources/application.properties` :

```properties
# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/citesignal?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=votre_username
spring.datasource.password=votre_password

# Email (exemple avec Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=votre-email@gmail.com
spring.mail.password=votre-mot-de-passe-application

# JWT Secret (changez cette clé en production !)
jwt.secret=votre-clé-secrète-très-longue-et-aléatoire
```

**Note pour Gmail**: Vous devez utiliser un "Mot de passe d'application" et non votre mot de passe habituel. Activez la validation en 2 étapes et générez un mot de passe d'application.

### 4. Compiler et lancer l'application

```bash
mvn clean install
mvn spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

## Fonctionnalités implémentées

### Inscription et authentification

- ✅ Inscription des citoyens avec formulaire sécurisé
- ✅ Vérification d'email par token
- ✅ Authentification JWT
- ✅ Gestion des rôles (CITOYEN, AGENT_MUNICIPAL, ADMINISTRATEUR)
- ✅ Connexion/déconnexion
- ✅ Protection CSRF

### Gestion des utilisateurs

- ✅ Consultation du profil utilisateur
- ✅ Historique des signalements (structure prête)
- ✅ Création d'agents et administrateurs par l'admin
- ✅ Gestion des utilisateurs (liste, suppression) pour les admins

## Structure du projet

```
citesignal/
├── src/
│   ├── main/
│   │   ├── java/com/citesignal/
│   │   │   ├── config/          # Configuration (DataInitializer)
│   │   │   ├── controller/      # Contrôleurs MVC et REST
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── model/           # Entités JPA (User, Role)
│   │   │   ├── repository/      # Repositories Spring Data JPA
│   │   │   ├── security/        # Configuration sécurité JWT
│   │   │   ├── service/         # Services métier
│   │   │   └── CiteSignalApplication.java
│   │   └── resources/
│   │       ├── templates/       # Vues Thymeleaf
│   │       └── application.properties
│   └── test/                    # Tests unitaires et d'intégration
└── pom.xml
```

## Utilisation

### Inscription d'un citoyen

1. Accéder à `http://localhost:8080/auth/register`
2. Remplir le formulaire (nom, prénom, email, mot de passe)
3. Vérifier l'email reçu et cliquer sur le lien de vérification
4. Se connecter avec les identifiants

### Connexion

1. Accéder à `http://localhost:8080/auth/login`
2. Saisir email et mot de passe
3. Accéder au tableau de bord

## API REST

### Inscription
```http
POST /api/auth/register
Content-Type: application/json

{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "password": "motdepasse123"
}
```

### Connexion
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "jean.dupont@example.com",
  "password": "motdepasse123"
}
```

## Sécurité

- ✅ Mots de passe hachés avec BCrypt
- ✅ JWT pour l'authentification stateless
- ✅ Protection CSRF activée
- ✅ Validation des entrées utilisateur
- ✅ Autorisations basées sur les rôles (@PreAuthorize)
- ✅ URLs sécurisées par rôle

## Prochaines étapes

- [ ] Implémentation de la déclaration d'incidents
- [ ] Gestion du workflow des incidents
- [ ] Notifications en temps réel (WebSockets)
- [ ] Tableaux de bord avec statistiques
- [ ] Recherche et filtrage avancés
- [ ] Upload et gestion de photos
- [ ] Géolocalisation avec Google Maps

## Auteur

Développé dans le cadre du cours Développement Web Avancé 3INLOG
