#!/bin/bash

# Infrastructure Bancaire - Configuration du monitoring
set -e

echo "Configuration du monitoring..."

# Attendre que Prometheus soit prêt
wait_for_prometheus() {
    echo "Attente de Prometheus..."
    
    # Attendre que Prometheus soit disponible
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=prometheus -n monitoring --timeout=600s
    
    PROMETHEUS_URL="http://$(minikube ip):30004"
    
    echo "Vérification de Prometheus à $PROMETHEUS_URL..."
    
    for i in {1..30}; do
        if curl -s -f "$PROMETHEUS_URL/api/v1/query?query=up" > /dev/null 2>&1; then
            echo "Prometheus est prêt!"
            return 0
        fi
        echo "Attente de Prometheus... ($i/30)"
        sleep 10
    done
    
    echo "Prometheus n'est pas accessible après 5 minutes"
    exit 1
}

# Attendre que Grafana soit prêt
wait_for_grafana() {
    echo "Attente de Grafana..."
    
    # Attendre que Grafana soit disponible
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=grafana -n monitoring --timeout=600s
    
    GRAFANA_URL="http://$(minikube ip):30003"
    
    echo "Vérification de Grafana à $GRAFANA_URL..."
    
    for i in {1..30}; do
        if curl -s -f "$GRAFANA_URL/api/health" > /dev/null 2>&1; then
            echo "Grafana est prêt!"
            return 0
        fi
        echo "Attente de Grafana... ($i/30)"
        sleep 10
    done
    
    echo "Grafana n'est pas accessible après 5 minutes"
    exit 1
}

# Configurer les règles Prometheus
configure_prometheus_rules() {
    echo "Configuration des règles Prometheus..."
    
    # Créer un ConfigMap avec les règles
    kubectl create configmap banking-prometheus-rules \
        --from-file=banking_rules.yml=monitoring/prometheus/banking-rules.yml \
        --namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
    
    echo "Règles Prometheus configurées"
}

# Configurer les dashboards Grafana
configure_grafana_dashboards() {
    echo "Configuration des dashboards Grafana..."
    
    GRAFANA_POD=$(kubectl get pods -n monitoring -l app.kubernetes.io/name=grafana -o jsonpath='{.items[0].metadata.name}')
    
    # Créer les dashboards
    echo "Import du dashboard Banking Overview..."
    kubectl exec -n monitoring $GRAFANA_POD -- curl -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer admin" \
        -d @/dev/stdin \
        http://localhost:3000/api/dashboards/db << 'DASHBOARD_JSON'
{
  "dashboard": {
    "id": null,
    "title": "Banking Infrastructure Overview",
    "tags": ["banking", "microservices", "overview"],
    "style": "dark",
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Services Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"auth-service|accounts-service|transactions-service|notifications-service|angular-ui\"}",
            "legendFormat": "{{job}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {
                  "color": "red",
                  "value": 0
                },
                {
                  "color": "green",
                  "value": 1
                }
              ]
            }
          }
        },
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 0
        }
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{job}} - {{method}} {{status}}"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec",
            "min": 0
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 0
        }
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
DASHBOARD_JSON

    echo "Dashboards Grafana configurés"
}

# Configurer les ServiceMonitors
configure_servicemonitors() {
    echo "Configuration des ServiceMonitors..."
    
    cat << 'SERVICEMONITOR_YAML' | kubectl apply -f -
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: banking-services
  namespace: monitoring
  labels:
    app: banking-services
spec:
  selector:
    matchLabels:
      app: banking-services
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: kafka-monitor
  namespace: monitoring
  labels:
    app: kafka
spec:
  selector:
    matchLabels:
      strimzi.io/cluster: banking-kafka
  endpoints:
  - port: tcp-prometheus
    interval: 30s
---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: postgresql-monitor
  namespace: monitoring
  labels:
    app: postgresql
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: postgresql
  endpoints:
  - port: http
    path: /metrics
    interval: 30s
SERVICEMONITOR_YAML

    echo "ServiceMonitors configurés"
}

# Configurer les alertes
configure_alertmanager() {
    echo "Configuration d'AlertManager..."
    
    cat << 'ALERTMANAGER_YAML' | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: alertmanager-config
  namespace: monitoring
data:
  alertmanager.yml: |
    global:
      smtp_smarthost: 'localhost:587'
      smtp_from: 'alerts@banking.local'
    
    route:
      group_by: ['alertname']
      group_wait: 10s
      group_interval: 10s
      repeat_interval: 1h
      receiver: 'web.hook'
      routes:
      - match:
          severity: critical
        receiver: 'critical-alerts'
      - match:
          severity: warning
        receiver: 'warning-alerts'
    
    receivers:
    - name: 'web.hook'
      webhook_configs:
      - url: 'http://localhost:5001/'
    
    - name: 'critical-alerts'
      webhook_configs:
      - url: 'http://localhost:5001/critical'
      slack_configs:
      - api_url: 'YOUR_SLACK_WEBHOOK_URL'
        channel: '#banking-alerts'
        title: 'Critical Alert: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
    
    - name: 'warning-alerts'
      webhook_configs:
      - url: 'http://localhost:5001/warning'
      slack_configs:
      - api_url: 'YOUR_SLACK_WEBHOOK_URL'
        channel: '#banking-warnings'
        title: 'Warning Alert: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
ALERTMANAGER_YAML

    echo "AlertManager configuré"
}

# Configurer les métriques personnalisées
configure_custom_metrics() {
    echo "Configuration des métriques personnalisées..."
    
    # Créer un ConfigMap avec les métriques personnalisées
    cat << 'METRICS_YAML' | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: custom-metrics-config
  namespace: monitoring
data:
  banking_metrics.yml: |
    # Métriques personnalisées pour l'infrastructure bancaire
    
    # Métriques de transactions
    - name: banking_transactions_total
      help: Total number of transactions processed
      type: counter
      labels:
        - transaction_type
        - status
        - service
    
    # Métriques de comptes
    - name: banking_accounts_total
      help: Total number of accounts
      type: gauge
      labels:
        - account_type
        - status
    
    # Métriques de notifications
    - name: banking_notifications_sent_total
      help: Total number of notifications sent
      type: counter
      labels:
        - notification_type
        - channel
        - status
    
    # Métriques de sécurité
    - name: banking_security_events_total
      help: Total number of security events
      type: counter
      labels:
        - event_type
        - severity
        - source
METRICS_YAML

    echo "Métriques personnalisées configurées"
}

# Configurer les logs centralisés
configure_logging() {
    echo "Configuration des logs centralisés..."
    
    # Déployer Fluentd pour la collecte de logs
    cat << 'FLUENTD_YAML' | kubectl apply -f -
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
  namespace: monitoring
  labels:
    app: fluentd
spec:
  selector:
    matchLabels:
      app: fluentd
  template:
    metadata:
      labels:
        app: fluentd
    spec:
      containers:
      - name: fluentd
        image: fluent/fluentd-kubernetes-daemonset:v1-debian-elasticsearch
        env:
        - name: FLUENT_ELASTICSEARCH_HOST
          value: "elasticsearch.monitoring.svc.cluster.local"
        - name: FLUENT_ELASTICSEARCH_PORT
          value: "9200"
        - name: FLUENT_ELASTICSEARCH_SCHEME
          value: "http"
        - name: FLUENT_ELASTICSEARCH_USER
          value: "elastic"
        - name: FLUENT_ELASTICSEARCH_PASSWORD
          value: "changeme"
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
FLUENTD_YAML

    echo "Logs centralisés configurés"
}

# Afficher les informations d'accès
show_monitoring_info() {
    echo ""
    echo "Configuration du monitoring terminée!"
    echo ""
    echo "Services de monitoring disponibles:"
    echo "====================================="
    
    MINIKUBE_IP=$(minikube ip)
    
    echo "Prometheus:"
    echo "  URL: http://$MINIKUBE_IP:30004"
    echo "  Query: http://$MINIKUBE_IP:30004/graph"
    echo ""
    echo "Grafana:"
    echo "  URL: http://$MINIKUBE_IP:30003"
    echo "  Username: admin"
    echo "  Password: admin123"
    echo ""
    echo "AlertManager:"
    echo "  URL: http://$MINIKUBE_IP:30005"
    echo ""
    echo "Dashboards disponibles:"
    echo "  - Banking Infrastructure Overview"
    echo "  - Kafka Monitoring"
    echo "  - PostgreSQL Monitoring"
    echo "  - Kubernetes Cluster Monitoring"
    echo ""
    echo "Alertes configurées:"
    echo "  - Service Down Alerts"
    echo "  - High Error Rate Alerts"
    echo "  - High Response Time Alerts"
    echo "  - Resource Usage Alerts"
    echo "  - Database Performance Alerts"
    echo "  - Kafka Performance Alerts"
}

# Fonction principale
main() {
    echo "Infrastructure Bancaire - Configuration du monitoring"
    echo "======================================================="
    
    wait_for_prometheus
    wait_for_grafana
    configure_prometheus_rules
    configure_grafana_dashboards
    configure_servicemonitors
    configure_alertmanager
    configure_custom_metrics
    configure_logging
    show_monitoring_info
}

main "$@"