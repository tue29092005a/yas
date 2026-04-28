pipeline {
    agent any

    tools {
        jdk 'jdk25'
        maven 'maven3'
    }

    stages {
        stage('Build & Test All Services') {
            matrix {
                axes {
                    axis {
                        name 'SERVICE_NAME'
                        values 'media', 'product', 'cart', 'location', 'order', 'customer', 'rating', 'inventory', 'tax', 'search'
                    }
                }
                stages {
                    stage('Build Phase') {
                        when {
                            anyOf {
                                changeset "${SERVICE_NAME}/**"
                                environment name: 'FORCE_BUILD_ALL', value: 'true'
                            }
                        }
                        steps {
                            echo "Đang Build service: ${SERVICE_NAME}..."
                            sh "mvn clean compile -pl ${SERVICE_NAME} -am"
                        }
                    }
                    stage('Test Phase') {
                        when {
                            anyOf {
                                changeset "${SERVICE_NAME}/**"
                                environment name: 'FORCE_BUILD_ALL', value: 'true'
                            }
                        }
                        steps {
                            echo "Đang Test và Đo lường độ phủ cho service: ${SERVICE_NAME}..."
                            sh "mvn clean test org.jacoco:jacoco-maven-plugin:prepare-agent org.jacoco:jacoco-maven-plugin:report -pl ${SERVICE_NAME} -am" 
                        }
                        post {
                            always {
                                junit allowEmptyResults: true, 
                                      testResults: "${SERVICE_NAME}/target/surefire-reports/*.xml"
                                      
                                jacoco(
                                    execPattern: "${SERVICE_NAME}/target/jacoco.exec",
                                    classPattern: "${SERVICE_NAME}/target/classes",
                                    sourcePattern: "${SERVICE_NAME}/src/main/java",
                                    changeBuildStatus: true,
                                    minimumLineCoverage: '70', 
                                    maximumLineCoverage: '75'       
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}