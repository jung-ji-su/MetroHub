pipeline {
    agent any

    environment {
        GHCR_USER  = 'jung-ji-su'
        GHCR_TOKEN = credentials('ghcr-token')   // GitHub PAT (write:packages scope)
        REGISTRY   = 'ghcr.io/jung-ji-su'
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    echo "Commit: ${env.GIT_COMMIT_SHORT}  Build: #${BUILD_NUMBER}"
                }
            }
        }

        // ── Test ──────────────────────────────────────────────────────────
        stage('Test') {
            parallel {
                stage('subway-api') {
                    steps {
                        dir('subway-api') {
                            sh 'chmod +x gradlew'
                            sh './gradlew clean test --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'subway-api/build/test-results/**/*.xml'
                        }
                    }
                }
                stage('subway-notification') {
                    steps {
                        dir('subway-notification') {
                            sh 'chmod +x gradlew'
                            sh './gradlew clean test --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'subway-notification/build/test-results/**/*.xml'
                        }
                    }
                }
                stage('subway-collector lint') {
                    steps {
                        dir('subway-collector') {
                            sh 'pip install --quiet --break-system-packages -r requirements.txt flake8'
                            sh 'flake8 . --max-line-length=120 --exclude=__pycache__,venv'
                        }
                    }
                }
                stage('subway-web') {
                    steps {
                        dir('subway-web') {
                            sh 'npm ci'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }

        // ── Docker Login ──────────────────────────────────────────────────
        stage('Docker Login') {
            when { expression { env.GIT_BRANCH ==~ /origin\/(master|main)/ } }
            steps {
                sh 'echo ${GHCR_TOKEN} | docker login ghcr.io -u ${GHCR_USER} --password-stdin'
            }
        }

        // ── Docker Build & Push ───────────────────────────────────────────
        stage('Docker Build & Push') {
            when { expression { env.GIT_BRANCH ==~ /origin\/(master|main)/ } }
            parallel {
                stage('subway-api') {
                    steps {
                        dir('subway-api') {
                            sh './gradlew build -x test --no-daemon'
                            sh "docker build -t ${REGISTRY}/metrohub-api:${BUILD_NUMBER} -t ${REGISTRY}/metrohub-api:latest ."
                            sh "docker push ${REGISTRY}/metrohub-api:${BUILD_NUMBER}"
                            sh "docker push ${REGISTRY}/metrohub-api:latest"
                        }
                    }
                }
                stage('subway-notification') {
                    steps {
                        dir('subway-notification') {
                            sh './gradlew build -x test --no-daemon'
                            sh "docker build -t ${REGISTRY}/metrohub-notification:${BUILD_NUMBER} -t ${REGISTRY}/metrohub-notification:latest ."
                            sh "docker push ${REGISTRY}/metrohub-notification:${BUILD_NUMBER}"
                            sh "docker push ${REGISTRY}/metrohub-notification:latest"
                        }
                    }
                }
                stage('subway-collector') {
                    steps {
                        dir('subway-collector') {
                            sh "docker build -t ${REGISTRY}/metrohub-collector:${BUILD_NUMBER} -t ${REGISTRY}/metrohub-collector:latest ."
                            sh "docker push ${REGISTRY}/metrohub-collector:${BUILD_NUMBER}"
                            sh "docker push ${REGISTRY}/metrohub-collector:latest"
                        }
                    }
                }
                stage('subway-web') {
                    steps {
                        dir('subway-web') {
                            sh "docker build -t ${REGISTRY}/metrohub-web:${BUILD_NUMBER} -t ${REGISTRY}/metrohub-web:latest ."
                            sh "docker push ${REGISTRY}/metrohub-web:${BUILD_NUMBER}"
                            sh "docker push ${REGISTRY}/metrohub-web:latest"
                        }
                    }
                }
            }
        }

        // ── Deploy to Kubernetes ──────────────────────────────────────────
        stage('Deploy') {
            when { expression { env.GIT_BRANCH ==~ /origin\/(master|main)/ } }
            steps {
                sh """
                    kubectl set image deployment/subway-api \
                        subway-api=${REGISTRY}/metrohub-api:${BUILD_NUMBER} \
                        -n metrohub

                    kubectl set image deployment/subway-notification \
                        subway-notification=${REGISTRY}/metrohub-notification:${BUILD_NUMBER} \
                        -n metrohub

                    kubectl set image deployment/subway-collector \
                        subway-collector=${REGISTRY}/metrohub-collector:${BUILD_NUMBER} \
                        -n metrohub

                    kubectl set image deployment/subway-web \
                        subway-web=${REGISTRY}/metrohub-web:${BUILD_NUMBER} \
                        -n metrohub

                    kubectl rollout status deployment/subway-api          -n metrohub --timeout=120s
                    kubectl rollout status deployment/subway-notification  -n metrohub --timeout=120s
                    kubectl rollout status deployment/subway-collector     -n metrohub --timeout=120s
                    kubectl rollout status deployment/subway-web           -n metrohub --timeout=120s
                """
            }
        }
    }

    post {
        success {
            echo "Build #${BUILD_NUMBER} (${GIT_COMMIT_SHORT}) deployed successfully"
        }
        failure {
            echo "Build #${BUILD_NUMBER} failed — check stage logs above"
        }
        always {
            script {
                try { sh 'docker logout ghcr.io' } catch (ignored) {}
            }
            cleanWs()
        }
    }
}
