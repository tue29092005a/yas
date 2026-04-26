pipeline {
    agent any 
    
    tools {
        jdk 'jdk21' 
        maven 'maven3'
    }

    stages {
        stage('Build & Test - Media Service') {
            when {
                changeset "media/**" 
            }
            steps {
                echo "Đang tiến hành Build và Test cho Media Service..."
                
                dir('media') {
                    sh 'mvn clean test'
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