# Infrastructure & DevOps

Documentation complète de l'infrastructure et des processus DevOps de la plateforme bancaire.

## Architecture Infrastructure

### Vue d'Ensemble
\`\`\`
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer                            │
│                   (Kubernetes Ingress)                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────────┐
│                 Kubernetes Cluster                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │   Frontend  │ │Microservices│ │ Monitoring  │           │
│  │   (Next.js) │ │ (Spring)    │ │(Prom/Graf)  │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
│                                                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │ PostgreSQL  │ │    Kafka    │ │    Redis    │           │
│  │ (Databases) │ │ (Messaging) │ │   (Cache)   │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
\`\`\`

## Environnements

### Développement Local
- **Docker Compose** pour les services de base
- **Services Spring Boot** en mode développement
- **Next.js** en mode développement
- **Base de données** H2/PostgreSQL locale

### Staging
- **Minikube** ou cluster Kubernetes léger
- **Images Docker** des services
- **Base de données** PostgreSQL dédiée
- **Monitoring** basique

### Production
- **Cluster Kubernetes** multi-nœuds
- **Haute disponibilité** pour tous les services
- **Bases de données** répliquées
- **Monitoring** et alerting complets
- **Sauvegardes** automatisées

## Containerisation

### Images Docker

#### Microservices (Spring Boot)
\`\`\`dockerfile
# backend/auth-service/Dockerfile
FROM openjdk:17-jre-slim

# Créer utilisateur non-root
RUN groupadd -r banking && useradd -r -g banking banking

# Copier l'application
COPY target/auth-service-*.jar app.jar

# Configuration
EXPOSE 8081
USER banking

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Démarrage
ENTRYPOINT ["java", "-jar", "/app.jar"]
\`\`\`

#### Frontend (Next.js)
\`\`\`dockerfile
# Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

FROM node:18-alpine AS runner
WORKDIR /app

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

CMD ["node", "server.js"]
\`\`\`

### Docker Compose (Développement)
\`\`\`yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: banking-postgres
    environment:
      POSTGRES_DB: banking
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: banking-kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: banking-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  redis:
    image: redis:7-alpine
    container_name: banking-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
\`\`\`

## Kubernetes

### Namespace
\`\`\`yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: banking-platform
  labels:
    name: banking-platform
    environment: production
\`\`\`

### ConfigMap
\`\`\`yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: banking-config
  namespace: banking-platform
data:
  # Database
  DB_HOST: "postgresql-service"
  DB_PORT: "5432"
  
  # Kafka
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
  
  # Redis
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
  
  # Application
  SPRING_PROFILES_ACTIVE: "production"
  LOG_LEVEL: "INFO"
\`\`\`

### Secrets
\`\`\`yaml
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: banking-secrets
  namespace: banking-platform
type: Opaque
data:
  # Base64 encoded values
  DB_USERNAME: YmFua2luZ191c2Vy
  DB_PASSWORD: YmFua2luZ19wYXNzd29yZA==
  JWT_SECRET: c3VwZXItc2VjcmV0LWp3dC1rZXk=
  REDIS_PASSWORD: cmVkaXNfcGFzc3dvcmQ=
\`\`\`

### Déploiements

#### Auth Service
\`\`\`yaml
# k8s/auth-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: banking-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: banking/auth-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: banking-config
              key: DB_HOST
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: banking-secrets
              key: DB_USERNAME
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: banking-platform
spec:
  selector:
    app: auth-service
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
\`\`\`

### Ingress
\`\`\`yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: banking-ingress
  namespace: banking-platform
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.securebank.com
    - securebank.com
    secretName: banking-tls
  rules:
  - host: api.securebank.com
    http:
      paths:
      - path: /api/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8081
      - path: /api/accounts
        pathType: Prefix
        backend:
          service:
            name: accounts-service
            port:
              number: 8082
  - host: securebank.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 3000
\`\`\`

## Monitoring

### Prometheus Configuration
\`\`\`yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
    - role: pod
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      action: keep
      regex: true
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)

  - job_name: 'banking-services'
    static_configs:
    - targets: 
      - 'auth-service:8081'
      - 'accounts-service:8082'
      - 'transactions-service:8083'
      - 'notifications-service:8084'
    metrics_path: '/actuator/prometheus'

alerting:
  alertmanagers:
  - static_configs:
    - targets:
      - alertmanager:9093
\`\`\`

### Grafana Dashboards
\`\`\`json
{
  "dashboard": {
    "title": "Banking Platform Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "95th percentile - {{service}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "Error rate - {{service}}"
          }
        ]
      }
    ]
  }
}
\`\`\`

### Alerting Rules
\`\`\`yaml
# monitoring/alert_rules.yml
groups:
- name: banking-platform
  rules:
  - alert: ServiceDown
    expr: up == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Service {{ $labels.instance }} is down"
      description: "{{ $labels.instance }} has been down for more than 1 minute."

  - alert: HighErrorRate
    expr: sum(rate(http_requests_total{status=~"5.."}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate on {{ $labels.service }}"
      description: "Error rate is {{ $value | humanizePercentage }} for {{ $labels.service }}"

  - alert: DatabaseConnectionsHigh
    expr: postgres_stat_activity_count > 80
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High number of database connections"
      description: "Database has {{ $value }} active connections"
\`\`\`

## CI/CD Pipeline

### GitHub Actions
\`\`\`yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run backend tests
      run: |
        cd backend/auth-service
        ./mvnw test
        cd ../accounts-service
        ./mvnw test
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
    
    - name: Install frontend dependencies
      run: npm ci
    
    - name: Run frontend tests
      run: npm test
    
    - name: Build frontend
      run: npm run build

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Login to DockerHub
      uses: docker/login-action@v2
      with:
        username: ${{ secrets.DOCKERHUB_USERNAME }}
        password: ${{ secrets.DOCKERHUB_TOKEN }}
    
    - name: Build and push auth-service
      uses: docker/build-push-action@v4
      with:
        context: ./backend/auth-service
        push: true
        tags: banking/auth-service:${{ github.sha }},banking/auth-service:latest
    
    - name: Build and push frontend
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: banking/frontend:${{ github.sha }},banking/frontend:latest

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Configure kubectl
      uses: azure/k8s-set-context@v1
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG }}
    
    - name: Deploy to Kubernetes
      run: |
        sed -i 's|banking/auth-service:latest|banking/auth-service:${{ github.sha }}|g' k8s/auth-service.yaml
        kubectl apply -f k8s/
        kubectl rollout status deployment/auth-service -n banking-platform
\`\`\`

## Sauvegardes

### Base de Données
\`\`\`bash
#!/bin/bash
# scripts/backup-database.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/postgresql"
DATABASES=("auth_db" "accounts_db" "transactions_db" "notifications_db")

mkdir -p $BACKUP_DIR

for db in "${DATABASES[@]}"; do
    echo "Backing up $db..."
    pg_dump -h localhost -U postgres $db | gzip > "$BACKUP_DIR/${db}_${DATE}.sql.gz"
    
    # Garder seulement les 7 derniers jours
    find $BACKUP_DIR -name "${db}_*.sql.gz" -mtime +7 -delete
done

echo "Backup completed: $DATE"
\`\`\`

### Kubernetes Resources
\`\`\`bash
#!/bin/bash
# scripts/backup-k8s.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/kubernetes"

mkdir -p $BACKUP_DIR

# Sauvegarder les ressources Kubernetes
kubectl get all -n banking-platform -o yaml > "$BACKUP_DIR/resources_${DATE}.yaml"
kubectl get configmaps -n banking-platform -o yaml > "$BACKUP_DIR/configmaps_${DATE}.yaml"
kubectl get secrets -n banking-platform -o yaml > "$BACKUP_DIR/secrets_${DATE}.yaml"

echo "Kubernetes backup completed: $DATE"
\`\`\`

## Sécurité

### Network Policies
\`\`\`yaml
# k8s/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-network-policy
  namespace: banking-platform
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
  - from:
    - podSelector: {}
  egress:
  - to:
    - podSelector: {}
  - to: []
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53
\`\`\`

### Pod Security Policy
\`\`\`yaml
# k8s/pod-security-policy.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: banking-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
\`\`\`

## Maintenance

### Scripts de Maintenance
\`\`\`bash
#!/bin/bash
# scripts/maintenance.sh

echo "🔧 Maintenance de la plateforme bancaire"

# 1. Nettoyage des logs anciens
echo "Nettoyage des logs..."
find /var/log/banking -name "*.log" -mtime +30 -delete

# 2. Nettoyage des images Docker inutilisées
echo "Nettoyage Docker..."
docker system prune -f

# 3. Vérification de l'espace disque
echo "Vérification espace disque..."
df -h

# 4. Vérification des services
echo "Vérification des services..."
kubectl get pods -n banking-platform

# 5. Mise à jour des certificats SSL
echo "Vérification certificats SSL..."
kubectl get certificates -n banking-platform

echo "✅ Maintenance terminée"
\`\`\`

### Monitoring de la Santé
\`\`\`bash
#!/bin/bash
# scripts/health-check.sh

SERVICES=("auth-service:8081" "accounts-service:8082" "transactions-service:8083" "notifications-service:8084")
FAILED=0

for service in "${SERVICES[@]}"; do
    if curl -f "http://$service/actuator/health" > /dev/null 2>&1; then
        echo "✅ $service - OK"
    else
        echo "❌ $service - FAILED"
        FAILED=1
    fi
done

if [ $FAILED -eq 1 ]; then
    echo "🚨 Certains services sont en échec!"
    exit 1
else
    echo "✅ Tous les services sont opérationnels"
fi
\`\`\`

## Troubleshooting

### Problèmes Courants

#### Services ne Démarrent Pas
\`\`\`bash
# Vérifier les logs
kubectl logs -f deployment/auth-service -n banking-platform

# Vérifier les événements
kubectl get events -n banking-platform --sort-by='.lastTimestamp'

# Vérifier les ressources
kubectl describe pod <pod-name> -n banking-platform
\`\`\`

#### Problèmes de Base de Données
\`\`\`bash
# Se connecter à PostgreSQL
kubectl exec -it postgresql-0 -n banking-platform -- psql -U postgres

# Vérifier les connexions
SELECT * FROM pg_stat_activity;

# Vérifier l'espace disque
SELECT pg_size_pretty(pg_database_size('auth_db'));
\`\`\`

#### Problèmes de Performance
\`\`\`bash
# Métriques CPU/Mémoire
kubectl top pods -n banking-platform

# Métriques détaillées
kubectl exec -it <pod-name> -n banking-platform -- top
\`\`\`

Cette documentation complète couvre tous les aspects de l'infrastructure et du déploiement de la plateforme bancaire SecureBank.
