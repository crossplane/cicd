def call() {
    // The continuous pipeline is used to build and publish new versions of the stack when changes are merged into
    // the master branch.
    pipeline {
        agent { label 'upbound-gce' }

        options {
            disableConcurrentBuilds()
            timestamps()
        }

        // Checks for unprocessed changes in the repo once per day
        // 'H' allows Jenkins to choose the time to balance the load
        triggers {
            pollSCM('H H * * *')
        }

        environment {
            DOCKER = credentials('dockerhub-upboundci')
            CROSSPLANE_CLI_RELEASE = 'master'
            // The promote channel is a tag which is used as a release channel. It's
            // updated whenever a new artifact is ready for that channel.
            PROMOTE_CHANNEL = 'master'
        }

        stages {
            stage('Prepare') {
                steps {
                    sh 'mkdir bin'
                    sh "curl -sL https://raw.githubusercontent.com/crossplane/crossplane-cli/${CROSSPLANE_CLI_RELEASE}/bootstrap.sh | env PREFIX=${WORKSPACE} RELEASE=${CROSSPLANE_CLI_RELEASE} bash"
                }
            }
            stage('Publish Release') {

                steps {
                    // The build step turns this into a "dirty" environment from the perspective of `git describe`,
                    // so we set the version once at the beginning and use it for both the build and publish steps.

                    sh """STACK_VERSION=\$( git describe --tags --dirty --always )
                          STACK_VERSION=\${STACK_VERSION} ./bin/kubectl-crossplane-stack-build
                          STACK_VERSION=\${STACK_VERSION} ./bin/kubectl-crossplane-stack-publish
                    """
                }
            }

            stage('Promote Release to Channel') {

                steps {
                    // Ideally we wouldn't be rebuilding and repushing (a true promote would use the same artifact),
                    // but this is easier to implement.

                    sh """STACK_VERSION=${PROMOTE_CHANNEL} ./bin/kubectl-crossplane-stack-build
                          STACK_VERSION=${PROMOTE_CHANNEL} ./bin/kubectl-crossplane-stack-publish
                    """
                }
            }

            stage('Sync images') {

                steps {
                    // We don't need to wait, and waiting takes up an executor slot
                    build job: '/crossplane/sync-stacks', wait: false
                }

            }
        }

        post {
            always {
                script {
                    deleteDir()
                }
            }
        }
    }
}
