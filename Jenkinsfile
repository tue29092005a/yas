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
                echo "Dang ttien hanh Build va Test cho Media Service..."
                dir('media') {
                    sh 'chmod +x gradlew' 
                    sh './gradlew build'
                }
            }
            post {
                always {
                    junit 'media/build/test-results/**/*.xml' 
                }
            }
        }
    }
}