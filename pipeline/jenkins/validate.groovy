pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
    }
    stages {

        stage('Verify') {
            parallel {
                stage('Verify back') {
                    environment {
                        MAVEN_HOME = '/usr/share/maven'
                    }
                    agent {
                        kubernetes {
                            label 'mystuff-validate-maven'
                            containerTemplate {
                                name 'maven'
                                image 'maven:3.5.4-jdk-11-slim'
                                ttyEnabled true
                                command 'cat'
                            }
                        }
                    }
                    stage('Checkout') {
                        steps {
                            script {
                                def branch = env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.GIT_BRANCH
                                git branch: "${branch}", credentialsId: 'github-app', url: 'https://github.com/Web-tree/mystuff.git'
                            }
                        }
                    }
                    stages {
                        stage('Validate') {
                            steps {
                                dir('back') {
                                    sh 'mvn -B clean verify -Dmaven.test.failure.ignore=true'
                                    junit '**/target/surefire-reports/**/*.xml'
                                }
                            }
                        }
                    }
                }
                stage('Verify front') {
                    agent {
                        kubernetes {
                            label 'mystuff-validate-node'
                            containerTemplate {
                                name 'maven'
                                image 'webtree/node-with-chrome'
                                ttyEnabled true
                                command 'cat'
                            }
                        }
                    }
                    stage('Checkout') {
                        steps {
                            script {
                                def branch = env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.GIT_BRANCH
                                git branch: "${branch}", credentialsId: 'github-app', url: 'https://github.com/Web-tree/mystuff.git'
                            }
                        }
                    }
                    stages {
                        stage('Validate') {
                            steps {
                                dir ('web/') {
                                    sh 'npm ci'
                                    sh 'npm run test-headless'
                                    junit 'testResult/*.xml'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
        failure {
            slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
    }
}