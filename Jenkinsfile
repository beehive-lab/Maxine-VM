pipeline {
    agent any
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }
    tools {
        jdk 'openJDK7u131'
    }
    environment {
        MAXINE_HOME="$WORKSPACE/maxine"
        GRAAL_HOME="$WORKSPACE/graal"
        MX="$GRAAL_HOME/mxtool/mx"
    }

    stages {
        stage('clone') {
            steps {
                // Clean up workspace
                step([$class: 'WsCleanup'])
                dir(env.MAXINE_HOME) {
                    checkout scm
                }
                dir(env.GRAAL_HOME) {
                    // Use ugly/advanced syntax to perform shallow clone
                    checkout([$class: 'GitSCM', branches: [[name: '6447333ed4c63cd6557f679cddcf8827a12d65bd']], extensions: [[$class: 'CloneOption', noTags: true, shallow: true]], userRemoteConfigs: [[credentialsId: 'orion_github', url: 'https://github.com/beehive-lab/Maxine-Graal-Internal.git']]])
                }
            }
        }
        stage('checkstyle-n-build') {
            steps {
                parallel 'checkstyle': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX --suite maxine checkstyle'
                    }
                }, 'build': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX build'
                    }
                }
            }
        }
        stage('image') {
            steps {
                dir(env.MAXINE_HOME) {
                    sh '$MX image'
                }
            }
        }
        stage('test-init') {
            steps {
                dir(env.MAXINE_HOME) {
                    sh '$MX jttgen'
                    sh '$MX canonicalizeprojects'
                }
            }
        }
        stage('test') {
            steps {
                dir(env.MAXINE_HOME) {
                    sh '$MX test -image-configs=java -tests=c1x,graal,junit,output'
                    sh '$MX test -maxvm-configs=jsr292 -image-configs=java -tests=jsr292'
                    sh '$MX test -image-configs=ss -tests=output:Hello+Catch+GC+WeakRef+Final'
                }
            }
        }
        stage('javatester') {
            steps {
                dir(env.MAXINE_HOME) {
                    sh '$MX test -image-configs=java -tests=javatester'
                }
            }
        }
    }
}
