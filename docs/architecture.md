# Architecture de la Plateforme Bancaire

## Vue d'Ensemble

La plateforme SecureBank suit une architecture microservices moderne avec séparation claire des responsabilités et communication asynchrone.

## Diagramme d'Architecture

\`\`\`
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │   Load Balancer │
│   (Next.js)     │◄──►│   (Ingress)     │◄──►│   (Kubernetes)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │ Auth Service │ │Accounts Svc │ │Trans. Svc  │
        │   (8081)     │ │   (8082)    │ │   (8083)   │
        └──────────────┘ └─────────────┘ └────────────┘
                │               │               │
                └───────────────┼───────────────┘
                                │
                        ┌───────▼──────┐
                        │Notifications │
                        │Service (8084)│
                        └──────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼──────┐    ┌───────────▼──────┐    ┌──────────▼─────┐
│ PostgreSQL   │    │     Kafka        │    │     Redis      │
│ (Databases)  │    │  (Event Bus)     │    │   (Cache)      │
└──────────────┘    └──────────────────┘    └────────────────┘
\`\`\`

## Microservices

### 1. Auth Service (Port 8081)
**Responsabilités:**
- Authentification et autorisation des utilisateurs
- Gestion des tokens JWT
- Contrôle d'accès basé sur les rôles (RBAC)
- Gestion des profils utilisateurs

**Technologies:**
- Spring Boot 3.2
- Spring Security 6
- JWT
- PostgreSQL
- Kafka Producer

**Endpoints principaux:**
- `POST /api/auth/signin` - Connexion
- `POST /api/auth/signup` - Inscription
- `POST /api/auth/refresh` - Renouvellement token
- `GET /api/auth/profile` - Profil utilisateur

### 2. Accounts Service (Port 8082)
**Responsabilités:**
- Gestion des comptes bancaires
- Opérations de dépôt/retrait
- Historique des comptes
- Gestion des soldes

**Technologies:**
- Spring Boot 3.2
- JPA/Hibernate
- PostgreSQL
- Kafka Producer/Consumer
- Feign Client

**Endpoints principaux:**
- `GET /api/accounts` - Liste des comptes
- `POST /api/accounts` - Création compte
- `POST /api/accounts/{id}/deposit` - Dépôt
- `POST /api/accounts/{id}/withdraw` - Retrait

### 3. Transactions Service (Port 8083)
**Responsabilités:**
- Traitement des transferts
- Gestion des paiements
- Historique des transactions
- Calcul des frais

**Technologies:**
- Spring Boot 3.2
- JPA/Hibernate
- PostgreSQL
- Kafka Producer/Consumer
- Feign Client
- Circuit Breaker (Resilience4j)

**Endpoints principaux:**
- `POST /api/transactions/transfer` - Transfert
- `POST /api/transactions/payment` - Paiement
- `GET /api/transactions/history` - Historique
- `GET /api/transactions/{id}` - Détail transaction

### 4. Notifications Service (Port 8084)
**Responsabilités:**
- Envoi d'emails
- Notifications SMS
- Notifications push
- Gestion des templates

**Technologies:**
- Spring Boot 3.2
- JavaMail
- Kafka Consumer
- PostgreSQL
- Template Engine

**Endpoints principaux:**
- `POST /api/notifications/send` - Envoi notification
- `GET /api/notifications/history` - Historique
- `POST /api/notifications/templates` - Gestion templates

## Communication Inter-Services

### Synchrone (HTTP/REST)
- Frontend ↔ Microservices
- Transactions Service ↔ Accounts Service (via Feign)

### Asynchrone (Kafka)
- Events de création de compte
- Events de transaction
- Events de notification
- Events d'audit

### Topics Kafka
- `user-events` - Événements utilisateur
- `account-events` - Événements compte
- `transaction-events` - Événements transaction
- `notification-events` - Événements notification

## Base de Données

### Stratégie Database-per-Service
Chaque microservice possède sa propre base de données PostgreSQL:

- `auth_db` - Données d'authentification
- `accounts_db` - Données des comptes
- `transactions_db` - Données des transactions
- `notifications_db` - Données des notifications

### Schémas Principaux

**Auth Service:**
\`\`\`sql
users (id, username, email, password, roles, created_at)
roles (id, name, description)
user_roles (user_id, role_id)
\`\`\`

**Accounts Service:**
\`\`\`sql
accounts (id, user_id, account_number, type, balance, status)
account_transactions (id, account_id, type, amount, description)
\`\`\`

**Transactions Service:**
\`\`\`sql
transactions (id, from_account, to_account, amount, type, status)
transaction_limits (id, user_id, daily_limit, monthly_limit)
\`\`\`

**Notifications Service:**
\`\`\`sql
notifications (id, user_id, type, channel, content, status)
notification_templates (id, name, subject, body, type)
\`\`\`

## Sécurité

### Authentification
- JWT tokens avec expiration
- Refresh tokens pour renouvellement
- Chiffrement des mots de passe (BCrypt)

### Autorisation
- Rôles: CUSTOMER, EMPLOYEE, MANAGER, ADMIN
- Contrôle d'accès granulaire par endpoint
- Validation des permissions métier

### Communication
- HTTPS obligatoire en production
- Chiffrement des communications inter-services
- Validation des tokens à chaque requête

## Monitoring et Observabilité

### Métriques (Prometheus)
- Métriques applicatives (compteurs, timers)
- Métriques système (CPU, mémoire, réseau)
- Métriques métier (transactions, comptes)

### Logs
- Logs structurés (JSON)
- Corrélation des requêtes (Trace ID)
- Niveaux de log configurables

### Tracing (Jaeger)
- Tracing distribué des requêtes
- Visualisation des dépendances
- Analyse des performances

## Scalabilité

### Horizontal Scaling
- Réplication des microservices
- Load balancing automatique
- Auto-scaling basé sur les métriques

### Performance
- Cache Redis pour les données fréquentes
- Connection pooling pour les bases de données
- Optimisation des requêtes SQL

### Résilience
- Circuit breakers pour les appels externes
- Retry policies avec backoff
- Health checks et auto-healing
