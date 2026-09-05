pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Runs the whole pyramid - unit, integration, system -
                // plus the 80% branch coverage gate on et.aau.clinic.core.
                // The Maven project lives under backend/ (frontend/ is a
                // separate, ungraded React UI with no Maven build step).
                dir('backend') {
                    sh 'mvn --batch-mode clean verify'
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'backend/target/surefire-reports/*.xml,backend/target/failsafe-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'backend/target/site/jacoco/**', allowEmptyArchive: true
        }
    }
}
