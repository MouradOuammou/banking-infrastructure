#!/bin/bash

# Infrastructure Bancaire - Configuration CI/CD
set -e

echo "Configuration du pipeline CI/CD..."

# Attendre que Jenkins soit prêt
wait_for_jenkins() {
    echo "Attente de Jenkins..."
    
    # Attendre que Jenkins soit disponible
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=jenkins -n jenkins --timeout=600s
    
    # Attendre que Jenkins soit complètement démarré
    JENKINS_URL="http://$(minikube ip):30000"
    
    echo "Vérification de Jenkins à $JENKINS_URL..."
    
    for i in {1..30}; do
        if curl -s -f "$JENKINS_URL/login" > /dev/null 2>&1; then
            echo "Jenkins est prêt!"
            return 0
        fi
        echo "Attente de Jenkins... ($i/30)"
        sleep 10
    done
    
    echo "Jenkins n'est pas accessible après 5 minutes"
    exit 1
}

# Configurer Jenkins
configure_jenkins() {
    echo "Configuration de Jenkins..."
    
    JENKINS_URL="http://$(minikube ip):30000"
    JENKINS_POD=$(kubectl get pods -n jenkins -l app.kubernetes.io/name=jenkins -o jsonpath='{.items[0].metadata.name}')
    
    # Créer le job Jenkins
    kubectl exec -n jenkins $JENKINS_POD -- curl -X POST \
        -H "Content-Type: application/xml" \
        -d @/dev/stdin \
        --user admin:admin123 \
        "$JENKINS_URL/createItem?name=banking-pipeline" << 'JENKINS_JOB_XML'
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@2.40">
  <description>Infrastructure Bancaire CI/CD Pipeline</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <hudson.plugins.discard__build.DiscardBuildProperty>
      <strategy class="hudson.plugins.discard__build.DiscardOldBuildStrategy">
        <daysToKeepStr>30</daysToKeepStr>
        <numToKeepStr>10</numToKeepStr>
        <artifactDaysToKeepStr>-1</artifactDaysToKeepStr>
        <artifactNumToKeepStr>-1</artifactNumToKeepStr>
      </strategy>
    </hudson.plugins.discard__build.DiscardBuildStrategy>
    </hudson.plugins.discard__build.DiscardBuildProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.80">
    <scm class="hudson.plugins.git.GitSCM" plugin="git@4.7.1">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>https://github.com/your-org/banking-infrastructure.git</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/main</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="list"/>
      <extensions/>
    </scm>
    <scriptPath>cicd/jenkins/Jenkinsfile</scriptPath>
    <lightweight>false</lightweight>
  </definition>
  <triggers>
    <hudson.triggers.SCMTrigger>
      <spec>H/5 * * * *</spec>
    </hudson.triggers.SCMTrigger>
  </triggers>
  <disabled>false</disabled>
</flow-definition>
JENKINS_JOB_XML

    echo "Job Jenkins créé"
}

# Installer les plugins Jenkins nécessaires
install_jenkins_plugins() {
    echo "Installation des plugins Jenkins..."
    
    JENKINS_POD=$(kubectl get pods -n jenkins -l app.kubernetes.io/name=jenkins -o jsonpath='{.items[0].metadata.name}')
    
    # Liste des plugins nécessaires
    PLUGINS=(
        "workflow-aggregator"
        "git"
        "docker-workflow"
        "kubernetes"
        "sonar"
        "jacoco"
        "htmlpublisher"
        "slack"
        "trivy"
        "performance"
    )
    
    # Installer chaque plugin
    for plugin in "${PLUGINS[@]}"; do
        echo "Installation du plugin: $plugin"
        kubectl exec -n jenkins $JENKINS_POD -- curl -X POST \
            --user admin:admin123 \
            --data "plugin.$plugin=" \
            "http://localhost:8080/pluginManager/installNecessaryPlugins"
    done
    
    echo "Plugins Jenkins installés"
}

# Configurer SonarQube
configure_sonarqube() {
    echo "Configuration de SonarQube..."
    
    # Attendre que SonarQube soit prêt
    kubectl wait --for=condition=ready pod -l app=sonarqube -n sonarqube --timeout=600s
    
    SONARQUBE_URL="http://$(minikube ip):30001"
    
    echo "Vérification de SonarQube à $SONARQUBE_URL..."
    
    for i in {1..30}; do
        if curl -s -f "$SONARQUBE_URL/api/system/status" > /dev/null 2>&1; then
            echo "SonarQube est prêt!"
            break
        fi
        echo "Attente de SonarQube... ($i/30)"
        sleep 10
    done
    
    # Créer un token SonarQube
    echo "Création du token SonarQube..."
    
    # Note: En production, vous devriez créer le token via l'interface web
    # ou utiliser l'API SonarQube avec les bonnes credentials
    
    echo "SonarQube configuré"
}

# Configurer Argo CD
configure_argocd() {
    echo "Configuration d'Argo CD..."
    
    # Attendre qu'Argo CD soit prêt
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=600s
    
    # Appliquer la configuration Argo CD
    kubectl apply -f cicd/argocd/banking-app.yaml
    
    echo "Argo CD configuré"
}

# Créer les secrets nécessaires
create_cicd_secrets() {
    echo "Création des secrets CI/CD..."
    
    # Secret pour SonarQube
    kubectl create secret generic sonar-token \
        --from-literal=token=sonar-token-123 \
        --namespace jenkins --dry-run=client -o yaml | kubectl apply -f -
    
    # Secret pour Docker Registry
    kubectl create secret generic docker-registry \
        --from-literal=username=admin \
        --from-literal=password=admin123 \
        --namespace jenkins --dry-run=client -o yaml | kubectl apply -f -
    
    # Secret pour Slack (optionnel)
    kubectl create secret generic slack-webhook \
        --from-literal=webhook=https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK \
        --namespace jenkins --dry-run=client -o yaml | kubectl apply -f -
    
    echo "Secrets CI/CD créés"
}

# Configurer les RBAC pour Jenkins
configure_jenkins_rbac() {
    echo "Configuration des RBAC pour Jenkins..."
    
    cat << 'RBAC_YAML' | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkins-service-account
  namespace: jenkins
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: jenkins-cluster-role
rules:
- apiGroups: [""]
  resources: ["pods", "pods/log", "pods/exec"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: [""]
  resources: ["services", "configmaps", "secrets"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["networking.k8s.io"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: jenkins-cluster-role-binding
subjects:
- kind: ServiceAccount
  name: jenkins-service-account
  namespace: jenkins
roleRef:
  kind: ClusterRole
  name: jenkins-cluster-role
  apiGroup: rbac.authorization.k8s.io
RBAC_YAML

    echo "RBAC Jenkins configuré"
}

# Afficher les informations d'accès
show_access_info() {
    echo ""
    echo "Configuration CI/CD terminée!"
    echo ""
    echo "Services CI/CD disponibles:"
    echo "=============================="
    
    MINIKUBE_IP=$(minikube ip)
    
    echo "Jenkins:"
    echo "  URL: http://$MINIKUBE_IP:30000"
    echo "  Username: admin"
    echo "  Password: admin123"
    echo ""
    echo "SonarQube:"
    echo "  URL: http://$MINIKUBE_IP:30001"
    echo "  Username: admin"
    echo "  Password: admin"
    echo ""
    echo "Argo CD:"
    echo "  URL: http://$MINIKUBE_IP:30002"
    echo "  Username: admin"
    echo "  Password: admin123"
    echo ""
    echo "Prochaines étapes:"
    echo "  1. Accédez à Jenkins et configurez le pipeline"
    echo "  2. Configurez SonarQube avec vos règles de qualité"
    echo "  3. Configurez Argo CD pour le déploiement GitOps"
    echo "  4. Testez le pipeline complet"
}

# Fonction principale
main() {
    echo "Infrastructure Bancaire - Configuration CI/CD"
    echo "==============================================="
    
    wait_for_jenkins
    configure_jenkins_rbac
    create_cicd_secrets
    install_jenkins_plugins
    configure_jenkins
    configure_sonarqube
    configure_argocd
    show_access_info
}

main "$@"