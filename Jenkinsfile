pipeline {
    agent any 
    
    tools {
        jdk 'jdk21' 
    }

    stages {
        stage('Build & Test - Media Service') {
            when {
                changeset "media/**" 
            }
            steps {
                echo "Đang tiến hành Build và Test cho Media Service..."
                
                dir('media') {
                    sh 'chmod +x ../mvnw' 
                    sh '../mvnw clean test'
                }
            }
            post {
                always {
                    junit 'media/target/surefire-reports/*.xml' 
                }
            }
        }
    }
}