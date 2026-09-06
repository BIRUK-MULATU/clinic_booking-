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

        stage('Mutation Testing') {
            steps {
                // PIT mutation testing on et.aau.clinic.core - fails the
                // build if the mutation score drops below the 90% threshold
                // configured in pom.xml.
                dir('backend') {
                    sh 'mvn --batch-mode test-compile org.pitest:pitest-maven:mutationCoverage'
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'backend/target/surefire-reports/*.xml,backend/target/failsafe-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'backend/target/site/jacoco/**,backend/target/pit-reports/**', allowEmptyArchive: true
        }
    }
}
