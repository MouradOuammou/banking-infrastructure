#!/bin/bash

# Infrastructure Bancaire - Déploiement des microservices
set -e

echo "Déploiement des microservices bancaires..."

# Vérifier que Minikube est en cours d'exécution
check_minikube() {
    echo "Vérification de Minikube..."
    
    if ! minikube status | grep -q "Running"; then
        echo "Minikube n'est pas en cours d'exécution"
        echo "Démarrez Minikube avec: minikube start"
        exit 1
    fi
    
    echo "Minikube est en cours d'exécution"
}

# Déployer l'API Gateway
deploy_api_gateway() {
    echo "Déploiement de l'API Gateway..."
    
    kubectl apply -f infrastructure/kubernetes/api-gateway.yaml
    
    kubectl wait --for=condition=available deployment/api-gateway -n banking --timeout=300s
    
    echo "API Gateway déployé"
}

# Déployer Auth Service
deploy_auth_service() {
    echo "Déploiement d'Auth Service..."
    
    kubectl apply -f infrastructure/kubernetes/auth-service.yaml
    
    kubectl wait --for=condition=available deployment/auth-service -n banking --timeout=300s
    
    echo "Auth Service déployé"
}

# Déployer Accounts Service
deploy_accounts_service() {
    echo "Déploiement d'Accounts Service..."
    
    kubectl apply -f infrastructure/kubernetes/accounts-service.yaml
    
    kubectl wait --for=condition=available deployment/accounts-service -n banking --timeout=300s
    
    echo "Accounts Service déployé"
}

# Déployer Transactions Service
deploy_transactions_service() {
    echo "Déploiement de Transactions Service..."
    
    kubectl apply -f infrastructure/kubernetes/transactions-service.yaml
    
    kubectl wait --for=condition=available deployment/transactions-service -n banking --timeout=300s
    
    echo "Transactions Service déployé"
}

# Déployer Notifications Service
deploy_notifications_service() {
    echo "Déploiement de Notifications Service..."
    
    kubectl apply -f infrastructure/kubernetes/notifications-service.yaml
    
    kubectl wait --for=condition=available deployment/notifications-service -n banking --timeout=300s
    
    echo "Notifications Service déployé"
}

# Déployer Angular UI
deploy_angular_ui() {
    echo "Déploiement d'Angular UI..."
    
    kubectl apply -f infrastructure/kubernetes/angular-ui.yaml
    
    kubectl wait --for=condition=available deployment/angular-ui -n banking --timeout=300s
    
    echo "Angular UI déployé"
}

# Configurer les hosts locaux
configure_hosts() {
    echo "Configuration des hosts locaux..."
    
    MINIKUBE_IP=$(minikube ip)
    
    cat << HOSTS_EOF > /tmp/banking-hosts
# Infrastructure Bancaire - Hosts locaux
$MINIKUBE_IP banking.local
$MINIKUBE_IP api.banking.local
$MINIKUBE_IP auth.banking.local
$MINIKUBE_IP accounts.banking.local
$MINIKUBE_IP transactions.banking.local
$MINIKUBE_IP notifications.banking.local
HOSTS_EOF

    echo "Ajoutez ces lignes à votre fichier /etc/hosts:"
    echo "================================================"
    cat /tmp/banking-hosts
    echo "================================================"
    echo ""
    echo "Commande pour ajouter automatiquement (nécessite sudo):"
    echo "sudo cat /tmp/banking-hosts >> /etc/hosts"
}

# Vérifier le statut des services
check_services_status() {
    echo "Vérification du statut des services..."
    
    echo ""
    echo "Services dans le namespace banking:"
    kubectl get services -n banking
    
    echo ""
    echo "Deployments dans le namespace banking:"
    kubectl get deployments -n banking
    
    echo ""
    echo "Pods dans le namespace banking:"
    kubectl get pods -n banking
}

# Afficher les URLs d'accès
show_access_urls() {
    echo ""
    echo "URLs d'accès aux services:"
    echo "============================="
    
    MINIKUBE_IP=$(minikube ip)
    
    echo "Frontend Angular UI:"
    echo "  http://banking.local"
    echo "  http://$MINIKUBE_IP (via NodePort)"
    echo ""
    echo "API Gateway:"
    echo "  http://api.banking.local"
    echo "  http://$MINIKUBE_IP (via NodePort)"
    echo ""
    echo "Services individuels:"
    echo "  Auth Service: http://auth.banking.local"
    echo "  Accounts Service: http://accounts.banking.local"
    echo "  Transactions Service: http://transactions.banking.local"
    echo "  Notifications Service: http://notifications.banking.local"
    echo ""
    echo "Monitoring:"
    echo "  Grafana: http://$MINIKUBE_IP:30003 (admin/admin123)"
    echo "  Prometheus: http://$MINIKUBE_IP:30004"
    echo ""
    echo "CI/CD:"
    echo "  Jenkins: http://$MINIKUBE_IP:30000 (admin/admin123)"
    echo "  SonarQube: http://$MINIKUBE_IP:30001 (admin/admin)"
    echo "  Argo CD: http://$MINIKUBE_IP:30002 (admin/admin123)"
}

# Fonction principale
main() {
    echo "Infrastructure Bancaire - Déploiement des microservices"
    echo "========================================================"
    
    check_minikube
    deploy_api_gateway
    deploy_auth_service
    deploy_accounts_service
    deploy_transactions_service
    deploy_notifications_service
    deploy_angular_ui
    configure_hosts
    check_services_status
    show_access_urls
    
    echo ""
    echo "Déploiement terminé avec succès!"
    echo ""
    echo "Prochaines étapes:"
    echo "  1. Ajoutez les hosts à votre fichier /etc/hosts"
    echo "  2. ./scripts/setup-cicd.sh"
    echo "  3. Accédez à l'application via http://banking.local"
}

main "$@"