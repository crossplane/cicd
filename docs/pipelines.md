# Pipelines

So you've used these libraries to set up some pipelines. Great! But
what's next? This will explain our common patterns and how to use them.

## Stack pipelines

For a stack repository, we typically create a few different jobs:

* `branch-create`
* `tag`
* `publish`
* `continuous-publish`

Each of them has a different purpose.

### What repository can I use as a reference?

Try out the [sample-stack-wordpress repo](https://github.com/crossplane/sample-stack-wordpress).
Take a look at the `Jenkinsfile.*` files.

### What else is needed other than Jenkinsfiles?

You'll also need to set up permissions and webhooks:

* The group `crossplane/upbound-bot` needs `Write` permissions for the
  repository so it can manage branches and tags during releases.
* There should be a webhook set up so Jenkins can run a continuous
  release when `master` is updated:
  - Payload URL: `https://jenkinsci.upbound.io/github-webhook/`
  - Content type: `application/x-www-form-urlencoded`
  - Secret: (blank)
  - SSL Verfication: `Enable SSL verification`
  - Which events would you like to trigger this webhook?: `Just the push event`


### I have a new repository, and I've added the appropriate Jenkinsfiles. How do I create the pipeline jobs?

Typically we copy from an existing item. The general steps are:

* Go to the [upbound-jenkins].
* Log in
* Create a new item in a folder matching the org name (typically
  `crossplane`).
* At the top, enter in the name of your repository, such as
  `stack-my-app`.
* At the bottom, in the `Copy from` field, enter in an existing stack
  folder, such as `sample-stack-wordpress`.
* Once the folder is created, go into each of the jobs that was created,
  and update the Git url to point to your repository.

### How do I cut a release?

Cutting a release involves: (potentially) cutting a new release branch;
tagging a commit for a release; building and publishing from the tagged
commit. Using the jobs, the steps are:

1. Using the job `branch-create`, create a release branch if one doesn't
   exist. We use the convention `release-MAJOR.MINOR`. For example:
   `release-0.1`. If a branch already exists for the release version
   prefix, update the branch with the new changes instead of creating a
   new one.
2. Using the job `tag`, tag the commit you want to release, with the
   release tag. The tag should be in the form `vMAJOR.MINOR.PATCH`. For
   example: `v0.1.0`.
3. Using the job `publish`, build and publish the tag you want to
   release. You should be able to do this using the same tag as what you
   used in the tagging step.

**NOTE - KNOWN ISSUE**: You may need to run a job multiple times when a
new branch is recognized by Jenkins. We are currently using multibranch
pipelines, which means Jenkins creates a new job for each branch that
matches. The first time the job runs on a branch, Jenkins does not know
what parameters are needed to run the job successfully, so it doesn't
let you specify any parameters, and the job may fail. After the first
execution, Jenkins will let you specify parameters, and you can run it
again with parameters. It isn't ideal, but that's the current pattern.

### How do I get my merges to master automatically released?

Once you've added the `Jenkinsfile.continuous` file, copied the
Jenkins jobs for your new repo, and added a webhook, Jenkins will
automatically build and publish your artifact when a pull request is
merged to master.

<!-- Reference-style links -->
[upbound-jenkins]: https://jenkinsci.upbound.io/job/crossplane/
