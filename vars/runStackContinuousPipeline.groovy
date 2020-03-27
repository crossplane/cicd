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
                    sh "curl -sL -o bin/semver https://raw.githubusercontent.com/fsaintjacques/semver-tool/3.0.0/src/semver && chmod +x bin/semver"
                }
            }
            stage('Publish Release') {

                steps {
                    sh 'docker login -u="${DOCKER_USR}" -p="${DOCKER_PSW}"'

                    // The build step turns this into a "dirty" environment from the perspective of `git describe`,
                    // so we set the version once at the beginning and use it for both the build and publish steps.

                    // The lines which calculate the stack version are a bit complicated, so here are some examples
                    // of the final version output for different scenarios.
                    //
                    // * No tags, one commit in the repository: v0.1.0-rc-1-gdeadbee
                    // * No tags, ten commits in the repository: v0.1.0-rc-10-gdeadbee
                    // * Most recent tag v0.1.0, target commit is same as tag: v0.2.0-rc-0-gdeadbee
                    // * Most recent tag v0.1.0, target commit is 2 commits after the tag: v0.2.0-rc-2-gdeadbee
                    // * Most recent tag v0.1.0-rc, target commit is 2 commits after the tag: v0.2.0-rc-2-gdeadbee
                    sh """STACK_VERSION=\$( git describe --tags --long || echo "v0.0.0-rc-\$( git log --oneline | wc -l | xargs echo )-g\$( git rev-parse --short=7 HEAD )" )
                          STACK_PREREL=\$( bin/semver get prerel \${STACK_VERSION} )

                          # In the case that our most recent tag was something like v0.1.0, our `git describe` output
                          # will look like 'v0.1.0-2-gdeadbee'. This makes our prerelease segment look more like
                          # 'rc-2-gdeadbee', instead of '2-gdeadbee'.
                          STACK_PREREL=\$( echo \${STACK_PREREL} | sed -e 's/\\(^[0-9]\\)/rc-\\1/' )

                          STACK_VERSION="v\$( bin/semver bump minor \${STACK_VERSION} )-\${STACK_PREREL}"
                          STACK_VERSION=\${STACK_VERSION} ./bin/kubectl-crossplane-stack-build
                          STACK_VERSION=\${STACK_VERSION} ./bin/kubectl-crossplane-stack-publish
                    """
                }
            }

            stage('Promote Release to Channel') {

                steps {
                    sh 'docker login -u="${DOCKER_USR}" -p="${DOCKER_PSW}"'

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
