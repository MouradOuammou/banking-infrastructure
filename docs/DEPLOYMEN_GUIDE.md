# Guide de Déploiement - Infrastructure Bancaire

## Vue d'ensemble

Cette infrastructure bancaire complète est basée sur des microservices Spring Boot avec un frontend Angular, déployée sur Minikube avec un pipeline CI/CD automatisé.

## Déploiement Rapide

### Prérequis
- Docker
- Minikube
- kubectl
- Helm 3.x
- Java 17+
- Node.js 18+
- Angular CLI

### Déploiement en une commande
```bash
./deploy-banking-infrastructure.sh
```

## Architecture

### Microservices Backend
- **Auth Service** (Port 8081): Authentification JWT et gestion des utilisateurs
- **Accounts Service** (Port 8082): Gestion des comptes et soldes
- **Transactions Service** (Port 8083): Virements, dépôts, retraits
- **Notifications Service** (Port 8084): Emails, SMS, push notifications

### Frontend
- **Angular UI** (Port 4200): Interface utilisateur complète

### Infrastructure
- **Kubernetes**: Orchestration avec Minikube
- **Apache Kafka**: Message broker avec Strimzi Operator
- **PostgreSQL**: Base de données principale
- **NGINX Ingress**: API Gateway et routage

### CI/CD
- **Jenkins**: Pipeline d'intégration continue
- **SonarQube**: Analyse de qualité du code
- **Argo CD**: Déploiement GitOps

### Monitoring
- **Prometheus**: Collecte des métriques
- **Grafana**: Dashboards et visualisation
- **AlertManager**: Gestion des alertes

## Configuration

### Variables d'environnement
```bash
# Base de données
SPRING_DATASOURCE_USERNAME=banking_user
SPRING_DATASOURCE_PASSWORD=banking123

# JWT
JWT_SECRET=banking-jwt-secret-key-2024

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=banking-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092
```

### Hosts locaux
Ajoutez ces lignes à votre fichier `/etc/hosts`:
```
<MINIKUBE_IP> banking.local
<MINIKUBE_IP> api.banking.local
<MINIKUBE_IP> auth.banking.local
<MINIKUBE_IP> accounts.banking.local
<MINIKUBE_IP> transactions.banking.local
<MINIKUBE_IP> notifications.banking.local
```

## Accès aux Services

### Application
- **Frontend**: http://banking.local
- **API Gateway**: http://api.banking.local

### DevOps
- **Jenkins**: http://\<MINIKUBE_IP\>:30000 (admin/admin123)
- **SonarQube**: http://\<MINIKUBE_IP\>:30001 (admin/admin)
- **Argo CD**: http://\<MINIKUBE_IP\>:30002 (admin/admin123)

### Monitoring
- **Grafana**: http://\<MINIKUBE_IP\>:30003 (admin/admin123)
- **Prometheus**: http://\<MINIKUBE_IP\>:30004
- **AlertManager**: http://\<MINIKUBE_IP\>:30005

## Tests

### Exécution des tests
```bash
./scripts/run-tests.sh
```

### Types de tests
- **Tests unitaires**: JUnit 5 + Mockito
- **Tests d'intégration**: TestContainers
- **Tests de performance**: JMeter
- **Tests de chaos**: Litmus
- **Tests de sécurité**: OWASP ZAP + Trivy

## Sécurité

### Fonctionnalités de sécurité
- **RBAC**: Rôles et permissions Kubernetes
- **Secrets**: Gestion sécurisée des secrets
- **Network Policies**: Isolation réseau
- **TLS**: Chiffrement des communications
- **Circuit Breaker**: Resilience4J pour la résilience
- **Audit**: Logging et monitoring des accès

### Rotation des secrets
Les secrets sont automatiquement rotés via un CronJob Kubernetes.

## Monitoring

### Métriques collectées
- **Performance**: Temps de réponse, débit, erreurs
- **Ressources**: CPU, mémoire, disque
- **Business**: Transactions, comptes, utilisateurs
- **Infrastructure**: Kubernetes, Kafka, PostgreSQL

### Alertes configurées
- Service Down
- High Error Rate
- High Response Time
- Resource Usage
- Security Events

## CI/CD Pipeline

### Étapes du pipeline
1. **Checkout**: Récupération du code
2. **Build**: Compilation et tests
3. **Quality Gate**: Analyse SonarQube
4. **Docker Build**: Création des images
5. **Security Scan**: Scan de vulnérabilités
6. **Deploy**: Déploiement via Argo CD
7. **Tests**: Tests d'intégration et performance

### Déclencheurs
- Push sur la branche main
- Pull Request
- Planification (tous les jours à 2h)

## Maintenance

### Commandes utiles
```bash
# Voir les logs
kubectl logs -f deployment/auth-service -n banking

# Redémarrer un service
kubectl rollout restart deployment/auth-service -n banking

# Accéder à un pod
kubectl exec -it <pod-name> -n banking -- /bin/bash

# Voir les métriques
kubectl top pods -n banking

# Voir les événements
kubectl get events -n banking --sort-by='.lastTimestamp'
```

### Sauvegarde
```bash
# Sauvegarde de la base de données
kubectl exec -it deployment/postgresql -n database -- pg_dump -U banking_user auth_service > auth_service_backup.sql

# Sauvegarde des secrets
kubectl get secrets -n banking -o yaml > secrets-backup.yaml
```

## Dépannage

### Problèmes courants

#### Service non accessible
```bash
# Vérifier le statut des pods
kubectl get pods -n banking

# Vérifier les logs
kubectl logs deployment/<service-name> -n banking

# Vérifier les services
kubectl get services -n banking
```

#### Problème de base de données
```bash
# Vérifier la connexion
kubectl exec -it deployment/postgresql -n database -- psql -U banking_user -d auth_service

# Vérifier les logs
kubectl logs deployment/postgresql -n database
```

#### Problème Kafka
```bash
# Vérifier les topics
kubectl exec -it banking-kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --list

# Vérifier les consumers
kubectl exec -it banking-kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## Évolutivité

### Scaling horizontal
```bash
# Augmenter le nombre de replicas
kubectl scale deployment auth-service --replicas=3 -n banking
```

### Scaling vertical
```bash
# Modifier les ressources
kubectl patch deployment auth-service -n banking -p '{"spec":{"template":{"spec":{"containers":[{"name":"auth-service","resources":{"limits":{"cpu":"1","memory":"1Gi"}}}]}}}}'
```

## Migration vers le Cloud

### Azure AKS
```bash
# Créer un cluster AKS
az aks create --resource-group banking-rg --name banking-aks --node-count 3 --enable-addons monitoring

# Obtenir les credentials
az aks get-credentials --resource-group banking-rg --name banking-aks

# Déployer l'infrastructure
./deploy-banking-infrastructure.sh
```

### AWS EKS
```bash
# Créer un cluster EKS
eksctl create cluster --name banking-eks --region us-west-2 --nodegroup-name banking-nodes --node-type t3.medium --nodes 3

# Déployer l'infrastructure
./deploy-banking-infrastructure.sh
```

## Support

### Documentation
- [Kubernetes](https://kubernetes.io/docs/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Angular](https://angular.io/docs)
- [Prometheus](https://prometheus.io/docs/)
- [Grafana](https://grafana.com/docs/)

### Contact
- **Développeur / Projet individuel**: mouradouammou8@gmail.com  
  *(Ce projet a été entièrement développé et réalisé par une seule personne.)*

## Licence

MIT License - Voir le fichier LICENSE pour plus de détails.