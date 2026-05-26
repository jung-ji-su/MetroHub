pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'metrohub'
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            parallel {
                stage('subway-api') {
                    steps {
                        dir('subway-api') {
                            sh 'chmod +x gradlew'
                            sh './gradlew clean build --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit 'subway-api/build/test-results/**/*.xml'
                        }
                    }
                }

                stage('subway-notification') {
                    steps {
                        dir('subway-notification') {
                            sh 'chmod +x gradlew'
                            sh './gradlew clean build --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit 'subway-notification/build/test-results/**/*.xml'
                        }
                    }
                }

                stage('subway-collector') {
                    steps {
                        dir('subway-collector') {
                            sh 'pip install --quiet -r requirements.txt'
                            sh 'pip install --quiet flake8'
                            sh 'flake8 . --max-line-length=120 --exclude=__pycache__'
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
                            sh "docker build -t ${DOCKER_REGISTRY}/subway-api:${BUILD_NUMBER} ."
                            sh "docker tag ${DOCKER_REGISTRY}/subway-api:${BUILD_NUMBER} ${DOCKER_REGISTRY}/subway-api:latest"
                        }
                    }
                }

                stage('subway-notification image') {
                    steps {
                        dir('subway-notification') {
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
    }

    post {
        success {
            echo "빌드 성공: #${BUILD_NUMBER}"
        }
        failure {
            echo "빌드 실패: #${BUILD_NUMBER}"
        }
        always {
            cleanWs()
        }
    }
}
