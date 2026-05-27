pipeline {
    agent any

    environment {
        DOCKER_REGISTRY  = credentials('DOCKER_REGISTRY')   // Docker Hub 레지스트리 (e.g. myusername)
        DOCKER_CREDS     = credentials('docker-hub-creds')   // Docker Hub 로그인 credential
        JWT_SECRET_DEV   = credentials('JWT_SECRET_DEV')
        DEPLOY_HOST      = credentials('DEPLOY_HOST')        // SSH 배포 서버 주소
        DEPLOY_USER      = 'ubuntu'
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    echo "Commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        stage('Build & Test') {
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
                            sh 'pip install --quiet -r requirements.txt flake8'
                            sh 'flake8 . --max-line-length=120 --exclude=__pycache__,venv'
                        }
                    }
                }

                stage('subway-web build') {
                    steps {
                        dir('subway-web') {
                            sh 'npm ci'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            parallel {
                stage('subway-api image') {
                    steps {
                        dir('subway-api') {
                            sh './gradlew build -x test --no-daemon'
                            sh "docker build -t ${DOCKER_REGISTRY}/subway-api:${BUILD_NUMBER} ."
                            sh "docker tag ${DOCKER_REGISTRY}/subway-api:${BUILD_NUMBER} ${DOCKER_REGISTRY}/subway-api:latest"
                        }
                    }
                }

                stage('subway-notification image') {
                    steps {
                        dir('subway-notification') {
                            sh './gradlew build -x test --no-daemon'
                            sh "docker build -t ${DOCKER_REGISTRY}/subway-notification:${BUILD_NUMBER} ."
                            sh "docker tag ${DOCKER_REGISTRY}/subway-notification:${BUILD_NUMBER} ${DOCKER_REGISTRY}/subway-notification:latest"
                        }
                    }
                }

                stage('subway-collector image') {
                    steps {
                        dir('subway-collector') {
                            sh "docker build -t ${DOCKER_REGISTRY}/subway-collector:${BUILD_NUMBER} ."
                            sh "docker tag ${DOCKER_REGISTRY}/subway-collector:${BUILD_NUMBER} ${DOCKER_REGISTRY}/subway-collector:latest"
                        }
                    }
                }
            }
        }

        stage('Docker Push') {
            when {
                anyOf {
                    branch 'master'
                    branch 'main'
                }
            }
            steps {
                sh "echo ${DOCKER_CREDS_PSW} | docker login -u ${DOCKER_CREDS_USR} --password-stdin"
                sh "docker push ${DOCKER_REGISTRY}/subway-api:${BUILD_NUMBER}"
                sh "docker push ${DOCKER_REGISTRY}/subway-api:latest"
                sh "docker push ${DOCKER_REGISTRY}/subway-notification:${BUILD_NUMBER}"
                sh "docker push ${DOCKER_REGISTRY}/subway-notification:latest"
                sh "docker push ${DOCKER_REGISTRY}/subway-collector:${BUILD_NUMBER}"
                sh "docker push ${DOCKER_REGISTRY}/subway-collector:latest"
            }
        }

        stage('Deploy') {
            when {
                anyOf {
                    branch 'master'
                    branch 'main'
                }
            }
            steps {
                sshagent(credentials: ['deploy-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} '
                            cd ~/MetroHub &&
                            docker-compose -f docker-compose.prod.yml pull &&
                            docker-compose -f docker-compose.prod.yml up -d --remove-orphans
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            echo "빌드 성공: #${BUILD_NUMBER} (${GIT_COMMIT_SHORT})"
        }
        failure {
            echo "빌드 실패: #${BUILD_NUMBER}"
        }
        always {
            sh 'docker logout || true'
            cleanWs()
        }
    }
}
