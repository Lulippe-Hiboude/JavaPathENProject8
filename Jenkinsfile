pipeline {
    agent any

    options {
        timestamps()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('TourGuide') {
                    sh 'mvn clean compile'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('TourGuide') {
                    sh 'mvn test'
                }
            }
        }

        stage('Package') {
            steps {
                dir('TourGuide') {
                    sh 'mvn package -DskipTests'
                }
            }
        }
    }

    post {
        success {
            echo 'Build SUCCESS'
        }
        failure {
            echo 'Build FAILED'
        }
        always {
            archiveArtifacts artifacts: 'TourGuide/target/*.jar', fingerprint: true
        }
    }
}
