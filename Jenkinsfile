pipeline {
    agent any 
    
    tools {
        jdk 'jdk25' 
        maven 'maven3' 
    }

    stages {
        stage('Build & Test - Media Service') {
            when {
                changeset "media/**" 
            }
            steps {
                echo "Đang tiến hành Build và Test cho Media Service..."
                sh 'mvn clean test -pl media -am'
            }
            post {
                always {
                    junit 'media/target/surefire-reports/*.xml' 
                }
            }
        }
    }
}