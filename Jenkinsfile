// うさぎ鉄道 電力・車両保守システム (URMS)  ビルドパイプライン
// Jenkins 2.x (交通システム部 CI サーバ: urms-ci01)  --  2017/09 移行 (Ant → Maven)
pipeline {
    agent { label 'rhel7-jdk8-cobol' }

    tools {
        jdk   'jdk1.8.0_202'
        maven 'maven-3.5.4'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
    }

    environment {
        MAVEN_OPTS   = '-Xmx1024m -Dfile.encoding=UTF-8'
        TZ           = 'Asia/Tokyo'
        NEXUS_URL    = 'http://nexus.usagi-rail.local:8081/repository/maven-releases/'
        WAS_HOST_STG = 'urms-was-stg01'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -s /opt/jenkins/settings.xml clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn -B -s /opt/jenkins/settings.xml test -Dtest=*Test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Test') {
            steps {
                sh 'mvn -B -s /opt/jenkins/settings.xml test -Dtest=*IT'
            }
        }

        stage('Batch Golden Test') {
            // gcc 4.8 / GnuCOBOL 2.2 は CI エージェントにプリインストール
            parallel {
                stage('C URPWD01')      { steps { sh 'cd batch/c && ./run.sh' } }
                stage('COBOL URINS01')  { steps { sh 'cd batch/cobol && ./run.sh' } }
            }
        }

        stage('Package WAR') {
            steps {
                sh 'mvn -B -s /opt/jenkins/settings.xml -DskipTests package'
                archiveArtifacts artifacts: 'target/*.war, batch/c/work/urpwd01, batch/cobol/work/urins01', fingerprint: true
            }
        }

        stage('Static Analysis') {
            when { branch 'develop' }
            steps {
                // FindBugs 3.0.1 / Checkstyle 6.x  (SonarQube 5.6 へ送信)
                sh 'mvn -B -s /opt/jenkins/settings.xml findbugs:findbugs checkstyle:checkstyle'
                sh 'mvn -B -s /opt/jenkins/settings.xml sonar:sonar -Dsonar.host.url=http://sonar.usagi-rail.local:9000'
            }
        }

        stage('Deploy to Staging (WebSphere)') {
            when { branch 'release/*' }
            steps {
                sh '''
                  scp target/usagi-railway-*.war wasadmin@${WAS_HOST_STG}:/opt/IBM/deploy/
                  ssh wasadmin@${WAS_HOST_STG} "/opt/IBM/WebSphere/AppServer/bin/wsadmin.sh -lang jython -f /opt/IBM/deploy/redeploy.py urms"
                  scp batch/c/work/urpwd01 batch/cobol/work/urins01 jp1adm@urms-bat-stg01:/opt/urms/bin/
                '''
            }
        }
    }

    post {
        failure {
            mail to: 'urms-dev-ml@usagi-rail.local',
                 subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} 失敗",
                 body: "ビルドが失敗しました. ${env.BUILD_URL}"
        }
        always {
            cleanWs()
        }
    }
}
