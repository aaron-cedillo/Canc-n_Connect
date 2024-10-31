pipeline {
    agent any
    environment {
        CREDENTIALS_ID = 'github-credentials' // ID de tus credenciales de GitHub en Jenkins
    }
    stages {
        stage('Checkout') {
            steps {
                git credentialsId: CREDENTIALS_ID, url: 'https://github.com/aaron-cedillo/Canc-n_Connect.git'
            }
        }
        stage('Build') {
            steps {
                bat 'echo Build successful'
                // Agrega aquí los comandos de construcción específicos para tu proyecto en Windows
            }
        }
        stage('Unit Tests') {
            steps {
                bat 'echo Running Unit Tests'
                // Agrega aquí los comandos para ejecutar tus pruebas unitarias en Windows
            }
        }
        stage('Static Code Analysis') {
            steps {
                bat 'echo Running Static Code Analysis'
                // Agrega aquí las herramientas de análisis estático compatibles en Windows
            }
        }
        stage('Deploy to Staging') {
            steps {
                bat 'echo Deploying to Staging'
                // Agrega aquí los pasos necesarios para el despliegue en el entorno de staging
            }
        }
        stage('Acceptance Tests') {
            steps {
                bat 'echo Running Acceptance Tests'
                // Comandos para pruebas de aceptación en Windows
            }
        }
        stage('Approval') {
            steps {
                input message: '¿Aprobar despliegue a producción?'
            }
        }
        stage('Deploy to Production') {
            steps {
                bat 'echo Deploying to Production'
                // Comandos para el despliegue en producción
            }
        }
    }
    post {
        success {
            echo 'Pipeline ejecutado exitosamente'
        }
        failure {
            echo 'El pipeline ha fallado'
        }
    }
}
