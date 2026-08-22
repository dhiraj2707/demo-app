pipeline {
    agent any
    tools {
        maven 'maven-3.9'
        jdk 'jdk-17'
    }
    environment {
        DOCKERHUB_USER = "bhau2707"   // <-- replace this
        IMAGE_NAME     = "demo-app"
        IMAGE_TAG      = "${env.BUILD_NUMBER}"
        FULL_IMAGE     = "${DOCKERHUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
    }
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build & Unit Test') {
            steps { sh 'mvn -B clean verify' }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('local-sonarqube') {
                    sh 'mvn -B sonar:sonar -Dsonar.projectKey=demo-app'
                }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('Docker Build') {
            steps { sh "docker build -t ${FULL_IMAGE} ." }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                      echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                      docker push ${FULL_IMAGE}
                      docker logout
                    '''
                }
            }
        }
        stage('Deploy to Dev') {
            steps {
                sh """
                  helm upgrade --install demo-app-dev ./helm-chart \
                    --namespace dev --create-namespace \
                    --set image.repository=${DOCKERHUB_USER}/${IMAGE_NAME} \
                    --set image.tag=${IMAGE_TAG}
                """
            }
        }
        stage('Smoke Test Dev') {
            steps {
                sh '''
                  kubectl run smoke-dev --rm -i --restart=Never -n dev --image=curlimages/curl -- \
                  sh -c "for i in \\$(seq 1 10); do curl -sf http://demo-app-dev:8080/health && exit 0; echo waiting...; sleep 3; done; exit 1"
                '''
            }
        }
        stage('Approval for Production') {
            steps {
                input message: "Deploy build #${IMAGE_TAG} to Production?", ok: 'Approve'
            }
        }
        stage('Deploy to Production') {
            steps {
                sh """
                  helm upgrade --install demo-app-prod ./helm-chart \
                    --namespace prod --create-namespace \
                    --set image.repository=${DOCKERHUB_USER}/${IMAGE_NAME} \
                    --set image.tag=${IMAGE_TAG}
                """
            }
        }
        stage('Smoke Test Prod') {
            steps {
                sh '''
                  kubectl run smoke-prod --rm -i --restart=Never -n prod --image=curlimages/curl -- \
                  sh -c "for i in \\$(seq 1 10); do curl -sf http://demo-app-prod:8080/health && exit 0; echo waiting...; sleep 3; done; exit 1"
                '''
            }
        }
    }
}
