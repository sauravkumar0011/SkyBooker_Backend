pipeline {

    agent any

    // ─────────────────────────────────────────────────────────
    // Global environment variables
    // ─────────────────────────────────────────────────────────
    environment {
        DOCKERHUB_USERNAME = 'sauravkumar0011'
        DOCKERHUB_CREDENTIALS = credentials('DOCKERHUB_CREDENTIALS')   // Jenkins credential ID
        EC2_HOST  = '13.233.178.135'
        EC2_USER  = 'ubuntu'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        REPO_URL  = 'https://github.com/sauravkumar0011/SkyBooker_Backend.git'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 45, unit: 'MINUTES')
    }

    // ─────────────────────────────────────────────────────────
    // STAGES
    // ─────────────────────────────────────────────────────────
    stages {

        // ── 1. CHECKOUT ──────────────────────────────────────
        stage('Checkout') {
            steps {
                echo "Cloning branch: ${env.BRANCH_NAME ?: 'main'}"
                git branch: 'main', url: "${REPO_URL}"
            }
        }

        // ── 2. BUILD JARs (parallel) ─────────────────────────
        stage('Build JARs') {
            parallel {
                stage('eureka-server') {
                    steps { dir('eureka-server')       { sh 'mvn package -DskipTests -q' } }
                }
                stage('api-gateway') {
                    steps { dir('api-gateway')         { sh 'mvn package -DskipTests -q' } }
                }
                stage('auth-service') {
                    steps { dir('auth-service')        { sh 'mvn package -DskipTests -q' } }
                }
                stage('flight-service') {
                    steps { dir('flight-service')      { sh 'mvn package -DskipTests -q' } }
                }
                stage('seat-service') {
                    steps { dir('seat-service')        { sh 'mvn package -DskipTests -q' } }
                }
                stage('booking-service') {
                    steps { dir('booking-service')     { sh 'mvn package -DskipTests -q' } }
                }
                stage('passenger-service') {
                    steps { dir('passenger-service')   { sh 'mvn package -DskipTests -q' } }
                }
                stage('payment-service') {
                    steps { dir('payment-service')     { sh 'mvn package -DskipTests -q' } }
                }
                stage('notification-service') {
                    steps { dir('notification-service'){ sh 'mvn package -DskipTests -q' } }
                }
                stage('airline-service') {
                    steps { dir('airline-service')     { sh 'mvn package -DskipTests -q' } }
                }
            }
        }

        // ── 3. BUILD DOCKER IMAGES ───────────────────────────
        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'eureka-server',
                        'api-gateway',
                        'auth-service',
                        'flight-service',
                        'seat-service',
                        'booking-service',
                        'passenger-service',
                        'payment-service',
                        'notification-service',
                        'airline-service'
                    ]
                    services.each { svc ->
                        echo "Building image: ${DOCKERHUB_USERNAME}/skybooker-${svc}:${IMAGE_TAG}"
                        sh """
                            docker build -t ${DOCKERHUB_USERNAME}/skybooker-${svc}:${IMAGE_TAG} \
                                         -t ${DOCKERHUB_USERNAME}/skybooker-${svc}:latest \
                                         ./${svc}
                        """
                    }
                }
            }
        }

        // ── 4. PUSH TO DOCKER HUB ────────────────────────────
        stage('Push to Docker Hub') {
            steps {
                script {
                    sh "echo ${DOCKERHUB_CREDENTIALS_PSW} | docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin"

                    def services = [
                        'eureka-server',
                        'api-gateway',
                        'auth-service',
                        'flight-service',
                        'seat-service',
                        'booking-service',
                        'passenger-service',
                        'payment-service',
                        'notification-service',
                        'airline-service'
                    ]
                    services.each { svc ->
                        echo "Pushing: ${DOCKERHUB_USERNAME}/skybooker-${svc}"
                        sh """
                            docker push ${DOCKERHUB_USERNAME}/skybooker-${svc}:${IMAGE_TAG}
                            docker push ${DOCKERHUB_USERNAME}/skybooker-${svc}:latest
                        """
                    }
                }
            }
        }

        // ── 5. DEPLOY TO EC2 ─────────────────────────────────
        stage('Deploy to EC2') {
            steps {
                sshagent(credentials: ['EC2_SSH_KEY']) {  // Jenkins credential ID

                    // Copy the prod compose file and .env to EC2
                    sh """
                        scp -o StrictHostKeyChecking=no \
                            docker-compose.prod.yml \
                            ${EC2_USER}@${EC2_HOST}:/home/${EC2_USER}/skybooker/docker-compose.prod.yml

                        scp -o StrictHostKeyChecking=no \
                            .env \
                            ${EC2_USER}@${EC2_HOST}:/home/${EC2_USER}/skybooker/.env
                    """

                    // SSH into EC2: pull latest images and restart containers
                    sh """
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} '
                            cd /home/${EC2_USER}/skybooker

                            echo "Logging into Docker Hub..."
                            echo "${DOCKERHUB_CREDENTIALS_PSW}" | docker login -u "${DOCKERHUB_CREDENTIALS_USR}" --password-stdin

                            echo "Pulling new images (tag: ${IMAGE_TAG})..."
                            IMAGE_TAG=${IMAGE_TAG} docker compose -f docker-compose.prod.yml pull

                            echo "Restarting containers..."
                            IMAGE_TAG=${IMAGE_TAG} docker compose -f docker-compose.prod.yml up -d --remove-orphans

                            echo "Cleaning up old images..."
                            docker image prune -f
                        '
                    """
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST ACTIONS
    // ─────────────────────────────────────────────────────────
    post {
        always {
            sh 'docker logout || true'
            cleanWs()
        }
        success {
            echo "✅ Pipeline SUCCESS — Build #${IMAGE_TAG} deployed to EC2 (${EC2_HOST})"
        }
        failure {
            echo "❌ Pipeline FAILED — Check the logs above"
        }
    }
}
