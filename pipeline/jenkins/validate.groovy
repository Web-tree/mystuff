def buildVersion
def backTag
def webTag
def tier
def isPullRequest

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
                    isPullRequest = env.CHANGE_ID ? true : false
                    branch = isPullRequest ? env.CHANGE_BRANCH : env.GIT_BRANCH
                    switch (branch) {
                        case 'master':
                            tier = 'prod'
                            break
                        default:
                            tier = 'dev'
                    }
                    def time = new Date().format('yyyyMMddHH.mm.ss')
                    webTag = "web-${tier}-${buildVersion}-${time}"
                    backTag = "back-${tier}-${buildVersion}-${time}"
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
                                dir('web/') {
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
                    parallel {
                        stage('Build web image') {
                            agent {
                                kubernetes {
                                    label 'mystuff-docker-builder'
                                    yamlFile 'pipeline/jenkins/agent/dockerBuilder.yaml'
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
                                }
                            }
                        }
                        stage('Build back image') {
                            agent {
                                kubernetes {
                                    label 'mystuff-docker-builder'
                                    yamlFile 'pipeline/jenkins/agent/dockerBuilder.yaml'
                                }
                            }
                            steps {
                                container('docker-builder') {
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
                    }

                }

                stage('Provision PR system') {
                    when {
                        expression { tier == 'dev' && isPullRequest }
                    }
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
                            deployDevEnv(buildVersion, webTag, backTag, "mystuff", tier)
                        }
                    }
                }

                stage('Update production') {
                    when {
                        expression { tier == 'prod' }
                    }
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
                            updateProduction(webTag, backTag, "mystuff")
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
    post {
        success {
            slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
        failure {
            slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
    }
}


private void deployDevEnv(buildVersion, webTag, backTag, projectName, tier) {
    def deployName = "${projectName}-${tier}-${buildVersion}"
    def webUrl = "${projectName}-${buildVersion}.dev.webtree.org"
    def backUrl = "back.${deployName}.webtree.org"
    sh "helm delete ${deployName} --purge || true"
    sh "helm install --wait --name=${deployName} --namespace=webtree-${tier} --set nameOverride=${deployName},ingress.web.host=${webUrl},ingress.back.host=${backUrl},images.web.tag=${webTag},images.back.tag=${backTag} -f values.${tier}.yaml neo4j.neo4jPassword=\$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 13 ; echo '') ."
    def message = "Test system provisioned on url https://${webUrl}. Backend: https://${backUrl}"
    sendPrComment("mystuff", env.CHANGE_ID, message)

}

private void updateProduction(webTag, backTag, projectName) {
    sh "helm upgrade --wait ${projectName} --reuse-values -f values.yaml --set images.web.tag=${webTag},images.back.tag=${backTag} ."
}

private void sendPrComment(repo, issueId, message) {
    def body = groovy.json.JsonOutput.toJson([body: message])
    httpRequest(consoleLogResponseBody: true,
        contentType: 'APPLICATION_JSON',
        httpMode: 'POST',
        requestBody: body,
        url: "https://api.github.com/repos/Web-tree/${repo}/issues/${issueId}/comments",
        authentication: 'github-repo-token',
        validResponseCodes: '201')
}