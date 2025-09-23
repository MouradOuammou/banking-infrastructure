#!/bin/bash

# Infrastructure Bancaire - Déploiement de l'infrastructure
set -e

echo "Déploiement de l'infrastructure bancaire..."

# Créer les namespaces
create_namespaces() {
    echo "Création des namespaces..."
    
    kubectl create namespace banking --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace database --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace jenkins --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace sonarqube --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
    
    echo "Namespaces créés"
}

# Déployer Kafka Cluster
deploy_kafka_cluster() {
    echo "Déploiement du cluster Kafka..."
    
    cat << 'KAFKA_YAML' | kubectl apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: banking-kafka
  namespace: kafka
spec:
  kafka:
    version: 3.5.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
      inter.broker.protocol.version: "3.5"
    storage:
      type: jbod
      volumes:
      - id: 0
        type: persistent-claim
        size: 10Gi
        deleteClaim: false
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 5Gi
      deleteClaim: false
  entityOperator:
    topicOperator: {}
    userOperator: {}
KAFKA_YAML

    kubectl wait --for=condition=ready pod -l strimzi.io/cluster=banking-kafka -n kafka --timeout=600s
    
    echo "Cluster Kafka déployé"
}

# Créer les topics Kafka
create_kafka_topics() {
    echo "Création des topics Kafka..."
    
    cat << 'TOPICS_YAML' | kubectl apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: transactions
  namespace: kafka
  labels:
    strimzi.io/cluster: banking-kafka
spec:
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000
    segment.ms: 604800000
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: notifications
  namespace: kafka
  labels:
    strimzi.io/cluster: banking-kafka
spec:
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000
    segment.ms: 604800000
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: audit-logs
  namespace: kafka
  labels:
    strimzi.io/cluster: banking-kafka
spec:
  partitions: 3
  replicas: 3
  config:
    retention.ms: 2592000000
    segment.ms: 2592000000
TOPICS_YAML

    echo "Topics Kafka créés"
}

# Déployer PostgreSQL
deploy_postgresql() {
    echo "Déploiement de PostgreSQL..."
    
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgresql -n database --timeout=300s
    
    kubectl exec -it deployment/postgresql -n database -- psql -U postgres -c "
        CREATE DATABASE auth_service;
        CREATE DATABASE accounts_service;
        CREATE DATABASE transactions_service;
        CREATE DATABASE notifications_service;
        CREATE USER banking_user WITH PASSWORD 'banking123';
        GRANT ALL PRIVILEGES ON DATABASE auth_service TO banking_user;
        GRANT ALL PRIVILEGES ON DATABASE accounts_service TO banking_user;
        GRANT ALL PRIVILEGES ON DATABASE transactions_service TO banking_user;
        GRANT ALL PRIVILEGES ON DATABASE notifications_service TO banking_user;
    "
    
    echo "PostgreSQL configuré"
}

# Déployer les secrets
deploy_secrets() {
    echo "Déploiement des secrets..."
    
    kubectl create secret generic database-secrets \
        --from-literal=username=banking_user \
        --from-literal=password=banking123 \
        --namespace banking --dry-run=client -o yaml | kubectl apply -f -
    
    kubectl create secret generic jwt-secrets \
        --from-literal=secret=banking-jwt-secret-key-2024 \
        --namespace banking --dry-run=client -o yaml | kubectl apply -f -
    
    kubectl create secret generic notification-secrets \
        --from-literal=smtp-username=banking@example.com \
        --from-literal=smtp-password=smtp123 \
        --from-literal=sms-api-key=sms123 \
        --namespace banking --dry-run=client -o yaml | kubectl apply -f -
    
    echo "Secrets déployés"
}

# Déployer les ConfigMaps
deploy_configmaps() {
    echo "Déploiement des ConfigMaps..."
    
    cat << 'CONFIGMAP_YAML' | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: banking-config
  namespace: banking
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: banking-service
      datasource:
        url: jdbc:postgresql://postgresql.database.svc.cluster.local:5432/
        username: banking_user
        password: banking123
        driver-class-name: org.postgresql.Driver
      jpa:
        hibernate:
          ddl-auto: update
        show-sql: false
        properties:
          hibernate:
            dialect: org.hibernate.dialect.PostgreSQLDialect
      kafka:
        bootstrap-servers: banking-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092
        consumer:
          group-id: banking-group
          auto-offset-reset: earliest
        producer:
          acks: all
          retries: 3
    logging:
      level:
        org.springframework.kafka: INFO
        com.banking: DEBUG
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: monitoring-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    rule_files:
      - "banking_rules.yml"
    
    scrape_configs:
      - job_name: 'banking-microservices'
        kubernetes_sd_configs:
          - role: endpoints
            namespaces:
              names:
                - banking
        relabel_configs:
          - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
            action: keep
            regex: true
          - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
            action: replace
            target_label: __metrics_path__
            regex: (.+)
CONFIGMAP_YAML

    echo "ConfigMaps déployés"
}

# Déployer les Network Policies
deploy_network_policies() {
    echo "Déploiement des Network Policies..."
    
    cat << 'NETPOL_YAML' | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-network-policy
  namespace: banking
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
    - namespaceSelector:
        matchLabels:
          name: monitoring
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - namespaceSelector:
        matchLabels:
          name: kafka
    ports:
    - protocol: TCP
      port: 9092
  - to: []
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53
NETPOL_YAML

    echo "Network Policies déployées"
}

# Déployer les RBAC
deploy_rbac() {
    echo "Déploiement des RBAC..."
    
    cat << 'RBAC_YAML' | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: banking-service-account
  namespace: banking
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: banking
  name: banking-role
rules:
- apiGroups: [""]
  resources: ["secrets", "configmaps"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: banking-role-binding
  namespace: banking
subjects:
- kind: ServiceAccount
  name: banking-service-account
  namespace: banking
roleRef:
  kind: Role
  name: banking-role
  apiGroup: rbac.authorization.k8s.io
RBAC_YAML

    echo "RBAC déployé"
}

# Fonction principale
main() {
    echo "Infrastructure Bancaire - Déploiement de l'infrastructure"
    echo "=========================================================="
    
    create_namespaces
    deploy_kafka_cluster
    create_kafka_topics
    deploy_postgresql
    deploy_secrets
    deploy_configmaps
    deploy_network_policies
    deploy_rbac
    
    echo ""
    echo "Infrastructure déployée avec succès!"
    echo ""
    echo "Services déployés:"
    echo "  - Kafka Cluster: 3 brokers + 3 zookeepers"
    echo "  - PostgreSQL: Base de données principale"
    echo "  - Secrets et ConfigMaps: Configuration sécurisée"
    echo "  - Network Policies: Isolation réseau"
    echo "  - RBAC: Contrôle d'accès"
    echo ""
    echo "Prochaines étapes:"
    echo "  ./scripts/deploy-microservices.sh"
}

main "$@"