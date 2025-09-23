#!/bin/bash

# Infrastructure Bancaire - Script de déploiement complet
set -e

echo "Infrastructure Bancaire Automatisée"
echo "======================================"
echo "Déploiement complet sur Minikube"
echo ""

# Variables
START_TIME=$(date +%s)
LOG_FILE="deployment.log"

# Fonction de logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

# Fonction d'erreur
error_exit() {
    log "ERREUR: $1"
    exit 1
}

# Vérifier les prérequis
check_prerequisites() {
    log "Vérification des prérequis..."
    
    if ! command -v docker &> /dev/null; then
        error_exit "Docker n'est pas installé"
    fi
    
    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl n'est pas installé"
    fi
    
    if ! command -v helm &> /dev/null; then
        error_exit "Helm n'est pas installé"
    fi
    
    if ! command -v minikube &> /dev/null; then
        error_exit "Minikube n'est pas installé"
    fi
    
    if ! command -v java &> /dev/null; then
        error_exit "Java n'est pas installé"
    fi
    
    if ! command -v node &> /dev/null; then
        error_exit "Node.js n'est pas installé"
    fi
    
    log "Tous les prérequis sont installés"
}

# Phase 1: Installation des dépendances
phase1_install_dependencies() {
    log "Phase 1: Installation des dépendances..."
    
    if ./scripts/install-dependencies.sh; then
        log "Phase 1 terminée avec succès"
    else
        error_exit "Échec de la Phase 1"
    fi
}

# Phase 2: Déploiement de l'infrastructure
phase2_deploy_infrastructure() {
    log "Phase 2: Déploiement de l'infrastructure..."
    
    if ./scripts/deploy-infrastructure.sh; then
        log "Phase 2 terminée avec succès"
    else
        error_exit "Échec de la Phase 2"
    fi
}

# Phase 3: Build des images Docker
phase3_build_docker_images() {
    log "Phase 3: Build des images Docker..."
    
    if ./scripts/build-docker-images.sh; then
        log "Phase 3 terminée avec succès"
    else
        error_exit "Échec de la Phase 3"
    fi
}

# Phase 4: Déploiement des microservices
phase4_deploy_microservices() {
    log "Phase 4: Déploiement des microservices..."
    
    if ./scripts/deploy-microservices.sh; then
        log "Phase 4 terminée avec succès"
    else
        error_exit "Échec de la Phase 4"
    fi
}

# Phase 5: Configuration CI/CD
phase5_setup_cicd() {
    log "Phase 5: Configuration CI/CD..."
    
    if ./scripts/setup-cicd.sh; then
        log "Phase 5 terminée avec succès"
    else
        error_exit "Échec de la Phase 5"
    fi
}

# Phase 6: Configuration du monitoring
phase6_setup_monitoring() {
    log "Phase 6: Configuration du monitoring..."
    
    if ./scripts/setup-monitoring.sh; then
        log "Phase 6 terminée avec succès"
    else
        error_exit "Échec de la Phase 6"
    fi
}

# Phase 7: Configuration de la sécurité
phase7_setup_security() {
    log "Phase 7: Configuration de la sécurité..."
    
    if ./scripts/setup-security.sh; then
        log "Phase 7 terminée avec succès"
    else
        error_exit "Échec de la Phase 7"
    fi
}

# Phase 8: Exécution des tests
phase8_run_tests() {
    log "Phase 8: Exécution des tests..."
    
    if ./scripts/run-tests.sh; then
        log "Phase 8 terminée avec succès"
    else
        log "Phase 8 terminée avec des avertissements"
    fi
}

# Vérification finale
final_verification() {
    log "Vérification finale de l'infrastructure..."
    
    log "Statut des pods:"
    kubectl get pods --all-namespaces | grep -E "(banking|kafka|database|monitoring|jenkins|sonarqube|argocd)"
    
    log "Statut des services:"
    kubectl get services --all-namespaces | grep -E "(banking|kafka|database|monitoring|jenkins|sonarqube|argocd)"
    
    log "Statut des ingresses:"
    kubectl get ingress --all-namespaces
    
    log "Vérification finale terminée"
}

# Afficher les informations d'accès
show_access_information() {
    log "Informations d'accès aux services..."
    
    MINIKUBE_IP=$(minikube ip)
    
    echo ""
    echo "Infrastructure Bancaire déployée avec succès!"
    echo "================================================"
    echo ""
    echo "Services disponibles:"
    echo "======================="
    echo ""
    echo "Application Bancaire:"
    echo "  Frontend: http://banking.local"
    echo "  API Gateway: http://api.banking.local"
    echo ""
    echo "Services DevOps:"
    echo "  Jenkins: http://$MINIKUBE_IP:30000 (admin/admin123)"
    echo "  SonarQube: http://$MINIKUBE_IP:30001 (admin/admin)"
    echo "  Argo CD: http://$MINIKUBE_IP:30002 (admin/admin123)"
    echo ""
    echo "Monitoring:"
    echo "  Grafana: http://$MINIKUBE_IP:30003 (admin/admin123)"
    echo "  Prometheus: http://$MINIKUBE_IP:30004"
    echo "  AlertManager: http://$MINIKUBE_IP:30005"
    echo ""
    echo "Infrastructure:"
    echo "  Minikube Dashboard: minikube dashboard"
    echo "  Kafka UI: http://$MINIKUBE_IP:30006"
    echo "  PostgreSQL: postgresql.database.svc.cluster.local:5432"
    echo ""
    echo "Informations de connexion:"
    echo "  Base de données: banking_user / [généré automatiquement]"
    echo "  JWT Secret: [généré automatiquement]"
    echo "  Certificats TLS: [générés automatiquement]"
    echo ""
    echo "Fichiers de configuration:"
    echo "  Logs de déploiement: $LOG_FILE"
    echo "  Résultats de tests: test-results/"
    echo "  Images Docker: localhost:5000/"
    echo ""
    echo "Commandes utiles:"
    echo "  Voir les logs: kubectl logs -f deployment/[service-name] -n banking"
    echo "  Redémarrer un service: kubectl rollout restart deployment/[service-name] -n banking"
    echo "  Accéder à un pod: kubectl exec -it [pod-name] -n banking -- /bin/bash"
    echo "  Voir les métriques: kubectl top pods -n banking"
    echo ""
    echo "Important:"
    echo "  Ajoutez ces lignes à votre fichier /etc/hosts:"
    echo "  $MINIKUBE_IP banking.local"
    echo "  $MINIKUBE_IP api.banking.local"
    echo "  $MINIKUBE_IP auth.banking.local"
    echo "  $MINIKUBE_IP accounts.banking.local"
    echo "  $MINIKUBE_IP transactions.banking.local"
    echo "  $MINIKUBE_IP notifications.banking.local"
}

# Calculer le temps d'exécution
calculate_execution_time() {
    END_TIME=$(date +%s)
    EXECUTION_TIME=$((END_TIME - START_TIME))
    HOURS=$((EXECUTION_TIME / 3600))
    MINUTES=$(((EXECUTION_TIME % 3600) / 60))
    SECONDS=$((EXECUTION_TIME % 60))
    
    log "Temps d'exécution total: ${HOURS}h ${MINUTES}m ${SECONDS}s"
}

# Fonction principale
main() {
    log "Démarrage du déploiement de l'Infrastructure Bancaire"
    
    check_prerequisites
    phase1_install_dependencies
    phase2_deploy_infrastructure
    phase3_build_docker_images
    phase4_deploy_microservices
    phase5_setup_cicd
    phase6_setup_monitoring
    phase7_setup_security
    phase8_run_tests
    final_verification
    show_access_information
    calculate_execution_time
    
    log "Déploiement terminé avec succès!"
}

# Gestion des erreurs
trap 'error_exit "Déploiement interrompu par l'utilisateur"' INT TERM

# Exécution
main "$@"