pipeline {
  options {
    buildDiscarder(logRotator(artifactNumToKeepStr: '1'))
  }
  agent {
    docker {
      image 'gradle:8.11-jdk21'
      args '--user root'
    }
  }
  stages {
    stage('Build') {
      steps {
        sh 'gradle build'
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts 'build/libs/*.jar'
        sh 'rm -rf build/libs/'
      }
    }
  }
}

