# banking-infrastructure

## Overview

Infrastructure bancaire complète basée sur des microservices Spring Boot avec frontend Angular, déployée sur Minikube avec CI/CD automatisé.

---

**Table of Contents**
* [Vue d'ensemble](#vue-densemble)
* [Stack Technologique](#stack-technologique)
* [Architecture des Microservices](#architecture-des-microservices)
* [Services et Ports](#services-et-ports)
* [Déploiement](#déploiement)
* [Monitoring et Observabilité](#monitoring-et-observabilité)
* [Sécurité](#sécurité)
* [Tests](#tests)
* [CI/CD Pipeline](#cicd-pipeline)

---

# Infrastructure Bancaire Automatisée

## Architecture Microservices Complète

---

### Vue d'ensemble

Infrastructure bancaire complète basée sur des microservices Spring Boot avec frontend Angular, déployée sur Minikube avec CI/CD automatisé.

---

### Stack Technologique

#### Backend Microservices (Spring Boot)
- **Auth Service**: Gestion JWT, rôles et permissions
- **Accounts Service**: Gestion des comptes et soldes
- **Transactions Service**: Virements, dépôts, retraits
- **Notifications Service**: Emails, SMS, push notifications

#### Frontend
- **Angular Dashboard**: Interface utilisateur complète

#### Infrastructure
- **Orchestration**: Kubernetes (Minikube)
- **Message Broker**: Apache Kafka (Strimzi Operator)
- **Base de données**: PostgreSQL
- **Conteneurisation**: Docker
- **CI/CD**: Jenkins + Argo CD (GitOps)
- **Monitoring**: Prometheus + Grafana
- **Sécurité**: SonarQube, RBAC, Resilience4J

---

### Architecture des Microservices

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Angular UI    │    │   API Gateway   │    │   Auth Service  │
│   (Frontend)    │◄──►│   (NGINX)       │◄──►│   (JWT/OAuth)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼─────┐
        │ Accounts     │ │Transactions │ │Notifications│
        │ Service      │ │ Service     │ │ Service    │
        └───────┬──────┘ └──────┬──────┘ └─────┬─────┘
                │               │               │
                └───────────────┼───────────────┘
                                │
                        ┌───────▼──────┐
                        │   PostgreSQL │
                        │   Database   │
                        └──────────────┘
                                │
                        ┌───────▼──────┐
                        │ Apache Kafka │
                        │ (Strimzi)    │
                        └──────────────┘
```

---

### Services et Ports

| Service | Port | Description |
|---------|------|-------------|
| Angular UI | 4200 | Interface utilisateur |
| API Gateway | 8080 | Point d'entrée API |
| Auth Service | 8081 | Authentification |
| Accounts Service | 8082 | Gestion comptes |
| Transactions Service | 8083 | Transactions |
| Notifications Service | 8084 | Notifications |
| PostgreSQL | 5432 | Base de données |
| Kafka | 9092 | Message broker |
| Jenkins | 8080 | CI/CD |
| SonarQube | 9000 | Analyse code |
| Prometheus | 9090 | Monitoring |
| Grafana | 3000 | Dashboards |

---

### Déploiement

#### Prérequis
- Minikube
- Helm 3.x
- Docker
- kubectl
- Java 17+
- Node.js 18+
- Angular CLI

#### Installation

```bash
# 1. Démarrer Minikube
minikube start --memory=8192 --cpus=4

# 2. Installer les dépendances
./scripts/install-dependencies.sh

# 3. Déployer l'infrastructure
./scripts/deploy-infrastructure.sh

# 4. Déployer les microservices
./scripts/deploy-microservices.sh

# 5. Configurer CI/CD
./scripts/setup-cicd.sh
```

---

### Monitoring et Observabilité

- **Métriques**: Prometheus collecte les métriques des microservices
- **Logs**: Centralisés via Fluentd
- **Tracing**: Distributed tracing avec Jaeger
- **Dashboards**: Grafana avec dashboards préconfigurés

---

### Sécurité

- **RBAC**: Rôles et permissions Kubernetes
- **Secrets**: Gestion sécurisée des secrets
- **Network Policies**: Isolation réseau
- **Circuit Breaker**: Resilience4J pour la résilience
- **SonarQube**: Analyse de sécurité du code

---

### Tests

- **Tests unitaires**: JUnit 5 + Mockito
- **Tests d'intégration**: TestContainers
- **Tests de performance**: JMeter
- **Chaos testing**: Litmus

---

### CI/CD Pipeline

1. **Build**: Compilation et tests
2. **Quality Gate**: SonarQube analysis
3. **Docker Build**: Création des images
4. **Deploy**: Argo CD GitOps
5. **Monitoring**: Validation du déploiement