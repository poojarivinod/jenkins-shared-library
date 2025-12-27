// search in google as "jenkins shared library example" -->  GSA/jenkins-shared-library-examples --> vars --> example1.groovy
// call is the default function
def call(Map configMap){
    pipeline {
        agent { label 'AGENT-1' }
        environment { 
            PROJECT = configMap.get('project')
            COMPONENT = configMap.get('component')
            appVersion = ''
            ACC_ID = '695862934667'
        }
        options {
            disableConcurrentBuilds()
            timeout(time: 30, unit: 'MINUTES')
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
        }
        stages {
            stage('Read Version') {
                steps {
                script{
                    def packageJson = readJSON file: 'package.json'
                    appVersion = packageJson.version
                    echo "Version is: $appVersion"
                }
                }
            }
            stage('Install Dependencies') {
                steps {
                script{ 
                    sh """
                        npm install
                    """
                }
                }
            }
            stage('Unit test'){
                steps {
                    script{ 
                        sh """
                            echo "Unit tests are created by developers, we will configure the command like npm test"
                        """
                    }
                }
            }
            /* stage('Run Sonarqube') {
                environment {
                    scannerHome = tool 'sonar-scanner-7.1';
                }
                steps {
                withSonarQubeEnv('sonar-scanner-7.1') {
                    sh "${scannerHome}/bin/sonar-scanner"
                    // This is generic command works for any language
                }
                }
            }
            stage("Quality Gate") {
                steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
                }
            } */
            stage('Docker Build') {
                steps {
                script{
                    withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                        sh """
                        aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com

                        docker build -t  ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion} .

                        docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion}
                        """
                    }
                    
                }
                }
            }
            stage('Trigger Deploy'){
                when { 
                    expression { params.deploy }
                }
                steps{
                    build job: "../${component}-cd", parameters: [string(name: 'version', value: "${appVersion}")], wait: true
                } // ../ means "one step back". pipeline come "one step back" form feature branch to EXPENSE folder
            }
        }
        post { 
            always { 
                echo 'I will always say Hello again!'
                deleteDir()
            }
            failure { 
                echo 'I will run when pipeline is failed'
            }
            success { 
                echo 'I will run when pipeline is success'
            }
        }
    }
}