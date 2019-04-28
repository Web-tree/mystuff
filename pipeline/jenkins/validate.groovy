import groovy.json.*

def buildVersion
def backTag
def webTag

pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
    }
    stages {
        stage('Env setup') {
            steps {
                script {
                    buildVersion = env.GIT_BRANCH.toLowerCase()
                    webTag = "web-${buildVersion}"
                    backTag = "back-${buildVersion}"
                }
            }
        }
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
                    stages {
                        stage('Checkout') {
                            steps {
                                script {
                                    def branch = env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.GIT_BRANCH
                                    git branch: "${branch}", credentialsId: 'github-app', url: 'https://github.com/Web-tree/mystuff.git'
                                }
                            }
                        }
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
                    stages {
                        stage('Checkout') {
                            steps {
                                script {
                                    def branch = env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.GIT_BRANCH
                                    git branch: "${branch}", credentialsId: 'github-app', url: 'https://github.com/Web-tree/mystuff.git'
                                }
                            }
                        }
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
        stage('Test system provision') {
            stages {
                stage('Build/publish images') {
                    agent {
                        kubernetes {
                            label 'mystuff-docker-builder'
                            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker-builder
    image: docker:18.09.5
    command: ['cat']
    tty: true
    volumeMounts:
    - name: dockersock
      mountPath: /var/run/docker.sock
  volumes:
  - name: dockersock
    hostPath:
      path: /var/run/docker.sock
"""
                        }
                    }
                    steps {
                        container('docker-builder') {
                            dir('web') {
                                script {
                                    withDockerRegistry(credentialsId: 'docker-hub') {

                                        def image = docker.build("webtree/mystuff:${webTag}")
                                        image.push(webTag)
                                    }
                                }
                            }
                            dir('back') {
                                script {
                                    withDockerRegistry(credentialsId: 'docker-hub') {

                                        def image = docker.build("webtree/mystuff:${backTag}")
                                        image.push(backTag)
                                    }
                                }
                            }
                        }
                    }
                }

                stage('Provision PR system') {
                    agent {
                        kubernetes {
                            label 'helm'
                            containerTemplate {
                                name 'helm'
                                image 'lachlanevenson/k8s-helm:v2.12.3'
                                ttyEnabled true
                                command 'cat'
                            }
                        }
                    }
                    steps {
                        dir('.kub/mystuff') {
                            script{
                                def deployName = "mystuff-${buildVersion}"
                                def webUrl = "mystuff-${buildVersion}.dev.webtree.org"
                                def backUrl = "back.mystuff-dev-${buildVersion}.webtree.org"
                                sh "helm delete ${deployName} --purge || true"
                                sh "helm install --wait --name=${deployName} --namespace=webtree-dev --set replicaCount=1 --set nameOverride=mystuff-${buildVersion} --set ingress.web.host=${webUrl} --set ingress.back.host=${backUrl} --set neo4j.core.persistentVolume.enabled=false --set images.web.tag=${webTag} --set images.back.tag=${backTag} --set images.back.pullPolicy=Always  --set images.web.pullPolicy=Always ."
                                def body = JsonOutput.toJson([body: "Test system provisioned on url https://${webUrl}. Backend: https://${backUrl}"])
                                httpRequest (consoleLogResponseBody: true,
                                    contentType: 'APPLICATION_JSON',
                                    httpMode: 'POST',
                                    requestBody: body,
                                    url: "https://api.github.com/repos/Web-tree/mystuff/issues/${env.CHANGE_ID}/comments",
                                    authentication: 'github-repo-token',
                                    validResponseCodes: '201')
                            }
                        }
                    }
                }

//                stage('e2e tests') {
//                    agent {
//                        kubernetes {
//                            label 'mystuff-validate-node'
//                            containerTemplate {
//                                name 'maven'
//                                image 'webtree/node-with-chrome'
//                                ttyEnabled true
//                                command 'cat'
//                            }
//                        }
//                    }
//                    stage('Checkout') {
//                            steps {
//                                script {
//                                    def branch = env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.GIT_BRANCH
//                                    git branch: "${branch}", credentialsId: 'github-app', url: 'https://github.com/Web-tree/mystuff.git'
//                                }
//                            }
//                        }
//                    steps {
//                        dir ('web/') {
//                            sh 'npm run e2e'
//                        }
//                    }
//                }
            }
        }
    }
//    post {
//        success {
//            slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
//        }
//        failure {
//            slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
//        }
//    }
}