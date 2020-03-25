def call() {
    // The publish pipeline is used to publish a single tagged image version of the stack.
    pipeline {
        agent { label 'upbound-gce' }

        parameters {
            string(name: 'version', description: 'Required. The version you are publishing. For example: v0.4.0.')
        }

        options {
            disableConcurrentBuilds()
            timestamps()
        }

        environment {
            DOCKER = credentials('dockerhub-upboundci')
            CROSSPLANE_CLI_RELEASE = 'master'
            // The promote channel is a tag which is used as a release channel. It's
            // updated whenever a new artifact is ready for that channel.
            PROMOTE_CHANNEL = 'alpha'
        }

        stages {
            stage('Check Required Parameters') {
                steps {
                    sh "test -n '${params.version}' || ( echo 'ERROR: The version is required. Please specify a version to release.' >&2 && exit 1 )"
                    sh "( echo '${params.version}' | grep -q -E '^v' - ) || ( echo 'ERROR: The version needs to start with a \"v\". Example: \"v0.1.0\".' >&2 && exit 1 )"
                }
            }

            stage('Prepare') {
                steps {
                    sh 'mkdir bin'
                    sh "curl -sL https://raw.githubusercontent.com/crossplane/crossplane-cli/${CROSSPLANE_CLI_RELEASE}/bootstrap.sh | env PREFIX=${WORKSPACE} RELEASE=${CROSSPLANE_CLI_RELEASE} bash"
                }
            }

            stage('Publish Release') {

                steps {
                    sh """
                          STACK_VERSION=${params.version}
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
