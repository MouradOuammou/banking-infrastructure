#!/bin/bash

# Infrastructure Bancaire - Installation des dépendances
set -e

echo "Installation des dépendances pour l'Infrastructure Bancaire..."

# Vérifier les prérequis
check_prerequisites() {
    echo "Vérification des prérequis..."
    
    # Vérifier Docker
    if ! command -v docker &> /dev/null; then
        echo "ERREUR: Docker n'est pas installé"
        exit 1
    fi
    
    # Vérifier kubectl
    if ! command -v kubectl &> /dev/null; then
        echo "ERREUR: kubectl n'est pas installé"
        exit 1
    fi
    
    # Vérifier Helm
    if ! command -v helm &> /dev/null; then
        echo "ERREUR: Helm n'est pas installé"
        exit 1
    fi
    
    # Vérifier Minikube
    if ! command -v minikube &> /dev/null; then
        echo "ERREUR: Minikube n'est pas installé"
        exit 1
    fi
    
    echo "SUCCES: Tous les prérequis sont installés"
}

# Démarrer Minikube
start_minikube() {
    echo "Démarrage de Minikube..."
    
    # Arrêter Minikube s'il est déjà en cours
    minikube stop 2>/dev/null || true
    
    # Démarrer Minikube avec les ressources nécessaires
    minikube start \
        --memory=8192 \
        --cpus=4 \
        --disk-size=20g \
        --driver=docker \
        --kubernetes-version=v1.28.0
    
    # Activer les addons nécessaires
    minikube addons enable ingress
    minikube addons enable metrics-server
    minikube addons enable dashboard
    
    echo "SUCCES: Minikube démarré avec succès"
}

# Installer Helm repositories
install_helm_repos() {
    echo "Installation des repositories Helm..."
    
    # Repository Strimzi pour Kafka
    helm repo add strimzi https://strimzi.io/charts/
    
    # Repository Prometheus
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    
    # Repository Grafana
    helm repo add grafana https://grafana.github.io/helm-charts
    
    # Repository NGINX Ingress
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    
    # Repository Jenkins
    helm repo add jenkins https://charts.jenkins.io
    
    # Repository SonarQube
    helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube
    
    # Repository Argo CD
    helm repo add argo https://argoproj.github.io/argo-helm
    
    # Mettre à jour les repositories
    helm repo update
    
    echo "SUCCES: Repositories Helm installés"
}

# Installer NGINX Ingress Controller
install_ingress() {
    echo "Installation de NGINX Ingress Controller..."
    
    helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
        --namespace ingress-nginx \
        --create-namespace \
        --set controller.service.type=NodePort \
        --set controller.service.nodePorts.http=30080 \
        --set controller.service.nodePorts.https=30443
    
    echo "SUCCES: NGINX Ingress Controller installé"
}

# Installer Strimzi Operator pour Kafka
install_kafka() {
    echo "Installation de Strimzi Operator pour Kafka..."
    
    # Installer Strimzi Operator
    helm upgrade --install strimzi strimzi/strimzi-kafka-operator \
        --namespace kafka \
        --create-namespace \
        --set watchAnyNamespace=true
    
    # Attendre que l'operator soit prêt
    kubectl wait --for=condition=ready pod -l name=strimzi-cluster-operator -n kafka --timeout=300s
    
    echo "SUCCES: Strimzi Operator installé"
}

# Installer PostgreSQL
install_postgresql() {
    echo "Installation de PostgreSQL..."
    
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo update
    
    helm upgrade --install postgresql bitnami/postgresql \
        --namespace database \
        --create-namespace \
        --set auth.postgresPassword=banking123 \
        --set auth.database=banking \
        --set primary.persistence.size=10Gi
    
    echo "SUCCES: PostgreSQL installé"
}

# Installer Prometheus
install_prometheus() {
    echo "Installation de Prometheus..."
    
    helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
        --namespace monitoring \
        --create-namespace \
        --set grafana.adminPassword=admin123 \
        --set prometheus.prometheusSpec.retention=30d \
        --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=10Gi
    
    echo "SUCCES: Prometheus installé"
}

# Installer Jenkins
install_jenkins() {
    echo "Installation de Jenkins..."
    
    helm upgrade --install jenkins jenkins/jenkins \
        --namespace jenkins \
        --create-namespace \
        --set controller.serviceType=NodePort \
        --set controller.serviceNodePort=30000 \
        --set controller.adminPassword=admin123 \
        --set persistence.size=10Gi
    
    echo "SUCCES: Jenkins installé"
}

# Installer SonarQube
install_sonarqube() {
    echo "Installation de SonarQube..."
    
    helm upgrade --install sonarqube sonarqube/sonarqube \
        --namespace sonarqube \
        --create-namespace \
        --set service.type=NodePort \
        --set service.nodePort=30001 \
        --set postgresql.enabled=true \
        --set postgresql.auth.postgresPassword=sonar123 \
        --set postgresql.auth.database=sonar \
        --set persistence.size=10Gi
    
    echo "SUCCES: SonarQube installé"
}

# Installer Argo CD
install_argocd() {
    echo "Installation d'Argo CD..."
    
    helm upgrade --install argocd argo/argo-cd \
        --namespace argocd \
        --create-namespace \
        --set server.service.type=NodePort \
        --set server.service.nodePort=30002 \
        --set configs.secret.argocdServerAdminPassword='$2a$10$rRyBsGSHK6.uc8fntPwVFOm0M1GdKOWVn8tDBenST2BQc7uqa.7zO' \
        --set global.domain=argocd.local
    
    echo "SUCCES: Argo CD installé"
}

# Fonction principale
main() {
    echo "Infrastructure Bancaire - Installation des dépendances"
    echo "======================================================"
    
    check_prerequisites
    start_minikube
    install_helm_repos
    install_ingress
    install_kafka
    install_postgresql
    install_prometheus
    install_jenkins
    install_sonarqube
    install_argocd
    
    echo ""
    echo "SUCCES: Installation terminée avec succès!"
    echo ""
    echo "Services disponibles:"
    echo "  - Minikube Dashboard: minikube dashboard"
    echo "  - Jenkins: http://$(minikube ip):30000 (admin/admin123)"
    echo "  - SonarQube: http://$(minikube ip):30001 (admin/admin)"
    echo "  - Argo CD: http://$(minikube ip):30002 (admin/admin123)"
    echo "  - Grafana: http://$(minikube ip):30003 (admin/admin123)"
    echo ""
    echo "Prochaines étapes:"
    echo "  ./scripts/deploy-infrastructure.sh"
}

main "$@"