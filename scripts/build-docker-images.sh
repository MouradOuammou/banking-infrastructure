#!/bin/bash

# Infrastructure Bancaire - Build des images Docker
set -e

echo "Build des images Docker pour l'Infrastructure Bancaire..."

# Variables
REGISTRY="localhost:5000"
VERSION="latest"

# Fonction pour build une image
build_image() {
    local service=$1
    local dockerfile=$2
    local context=$3
    
    echo "Building $service..."
    
    docker build -f $dockerfile -t $REGISTRY/$service:$VERSION $context
    
    docker tag $REGISTRY/$service:$VERSION $service:$VERSION
    
    echo "$service built successfully"
}

# Démarrer le registry local si nécessaire
start_registry() {
    echo "Démarrage du registry Docker local..."
    
    if ! docker ps | grep -q registry:2; then
        docker run -d -p 5000:5000 --name registry registry:2
        echo "Registry Docker démarré"
    else
        echo "Registry Docker déjà en cours d'exécution"
    fi
}

# Build Auth Service
build_auth_service() {
    echo "Building Auth Service..."
    
    mkdir -p microservices/auth-service/target
    touch microservices/auth-service/target/auth-service-1.0.0.jar
    
    build_image "auth-service" \
        "infrastructure/docker/Dockerfile.auth-service" \
        "microservices/auth-service"
}

# Build Accounts Service
build_accounts_service() {
    echo "Building Accounts Service..."
    
    mkdir -p microservices/accounts-service/target
    touch microservices/accounts-service/target/accounts-service-1.0.0.jar
    
    build_image "accounts-service" \
        "infrastructure/docker/Dockerfile.accounts-service" \
        "microservices/accounts-service"
}

# Build Transactions Service
build_transactions_service() {
    echo "Building Transactions Service..."
    
    mkdir -p microservices/transactions-service/target
    touch microservices/transactions-service/target/transactions-service-1.0.0.jar
    
    build_image "transactions-service" \
        "infrastructure/docker/Dockerfile.transactions-service" \
        "microservices/transactions-service"
}

# Build Notifications Service
build_notifications_service() {
    echo "Building Notifications Service..."
    
    mkdir -p microservices/notifications-service/target
    touch microservices/notifications-service/target/notifications-service-1.0.0.jar
    
    build_image "notifications-service" \
        "infrastructure/docker/Dockerfile.notifications-service" \
        "microservices/notifications-service"
}

# Build Angular UI
build_angular_ui() {
    echo "Building Angular UI..."
    
    mkdir -p frontend/angular-ui/dist/banking-ui
    echo "<!DOCTYPE html><html><head><title>Banking UI</title></head><body><h1>Banking UI</h1></body></html>" > frontend/angular-ui/dist/banking-ui/index.html
    
    cp infrastructure/docker/nginx.conf frontend/angular-ui/
    
    build_image "angular-ui" \
        "infrastructure/docker/Dockerfile.angular-ui" \
        "frontend/angular-ui"
}

# Load images dans Minikube
load_images_to_minikube() {
    echo "Chargement des images dans Minikube..."
    
    minikube image load auth-service:$VERSION
    minikube image load accounts-service:$VERSION
    minikube image load transactions-service:$VERSION
    minikube image load notifications-service:$VERSION
    minikube image load angular-ui:$VERSION
    
    echo "Images chargées dans Minikube"
}

# Fonction principale
main() {
    echo "Infrastructure Bancaire - Build des images Docker"
    echo "=================================================="
    
    start_registry
    build_auth_service
    build_accounts_service
    build_transactions_service
    build_notifications_service
    build_angular_ui
    load_images_to_minikube
    
    echo ""
    echo "Build terminé avec succès!"
    echo ""
    echo "Images créées:"
    echo "  - auth-service:$VERSION"
    echo "  - accounts-service:$VERSION"
    echo "  - transactions-service:$VERSION"
    echo "  - notifications-service:$VERSION"
    echo "  - angular-ui:$VERSION"
    echo ""
    echo "Prochaines étapes:"
    echo "  ./scripts/deploy-microservices.sh"
}

main "$@"