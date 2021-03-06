def call() {
    // The branch pipeline cuts a release branch.
    pipeline {
        agent { label 'upbound-gce' }

        parameters {
            string(name: 'branch', defaultValue: '', description: 'Required. The release branch to create. For example: "release-0.0". This should typically be "release-MAJOR.MINOR".')
            string(name: 'commit', defaultValue: '', description: 'Optional. Commit hash to use as the head of the branch. For example: 56b65dba917e50132b0a540ae6ff4c5bbfda2db6. If empty, the latest commit hash on the current branch will be used.')
        }

        options {
            disableConcurrentBuilds()
            timestamps()
        }

        environment {
            GITHUB_UPBOUND_BOT = credentials('github-upbound-jenkins')
        }

        stages {

            stage('Prepare') {
                steps {
                     // github credentials are not setup to push over https in jenkins. add the github token to the url
                    sh "git config remote.origin.url https://${GITHUB_UPBOUND_BOT_USR}:${GITHUB_UPBOUND_BOT_PSW}@\$(git config --get remote.origin.url | sed -e 's/https:\\/\\///')"
                    sh 'git config user.name "upbound-bot"'
                    sh 'git config user.email "info@crossplane.io"'
                    sh 'echo "machine github.com login upbound-bot password $GITHUB_UPBOUND_BOT" > ~/.netrc'
                }
            }

            stage('Tag Release') {
                steps {
                    sh "test -n '${params.branch}' || ( echo 'Branch is required - please enter a branch!' && exit 1 )"
                    // If the commit is not passed, it'll be empty, which means git will use the head
                    // of the current branch. Most of the time, the default behavior will be used (from master).
                    //
                    // For the push, we're assuming the remote is named "origin", but this is the convention,
                    // so it's a safe assumption for this situation.
                    sh """git branch ${params.branch} ${params.commit}
                          git push origin ${params.branch}
                    """
                }
            }
        }

        post {
            always {
                deleteDir()
            }
        }
    }
}
