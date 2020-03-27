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

Try out the [app-wordpress
repo](https://github.com/crossplane/app-wordpress).
Take a look at the `Jenkinsfile.*` files. To see the Jenkins jobs that
result, take a look at the [app-wordpress Jenkins folder on the Upbound
Jenkins](https://jenkinsci.upbound.io/job/crossplane/job/app-wordpress/).

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
  folder, such as `app-wordpress`.
* Once the folder is created, go into each of the jobs that was created,
  and update the Git url to point to your repository.

### What about docker credentials?

The shared Jenkins code takes care of that for us. It loads the docker
credentials early in its configuration, and then does a `docker login`
before docker operations, using environment variables set by Jenkins
when the docker credentials are loaded.

### How do I cut a release?

Cutting a release involves: tagging a commit for a release; building and
publishing from the tagged commit. Using the jobs, the steps are:

1. Using the job `tag`, tag the `master` branch with the release tag.
   The tag should be in the form `vMAJOR.MINOR.PATCH`. For example:
   `v0.1.0`.
2. Using the job `publish`, build and publish the tag you want to
   release. You should be able to do this using the same tag as what you
   used in the tagging step.

**Release branches**: We do not use release branches for a release in
most scenarios. We originally did, and realized we were not getting any
benefit from them, so we removed them from the process to simplify
things. If we end up needing them in the future, we can change the
process to be more robust. Release branches would become useful if we
needed to develop and maintain multiple distinct versions at the same
time. They're also useful for doing a patch release of a version which
was not the most recently released version.

**NOTE - KNOWN ISSUE**: You may need to run a job multiple times when a
new branch is recognized by Jenkins. We are currently using multibranch
pipelines, which means Jenkins creates a new job for each branch that
matches. The first time the job runs on a branch, Jenkins does not know
what parameters are needed to run the job successfully, so it doesn't
let you specify any parameters, and the job may fail. After the first
execution, Jenkins will let you specify parameters, and you can run it
again with parameters. It isn't ideal, but that's the current pattern.

#### Patch Releases

A patch release is a release with bug fixes, where the patch version is
incremented. For example, a patch release after `v0.1.0` would be
`v0.1.1`.

If the patch release is a patch of the most recently released version,
the process is the same as a normal release.

If the patch is for a version before the most recent release, a release
branch will be needed.

To create a release branch, use the job `branch-create`, and use the
tag that we want to create a patch release from. For example, if we
previously released `v0.1.0`, and we want to cut the patch release
`v0.1.1`, create the branch from the `v0.1.0` tag. We use the convention
`release-MAJOR.MINOR` for naming release branches. For example:
`release-0.1`. If a branch already exists for the release version
prefix, update the branch with the new changes instead of creating a new
one.

### How do I get my merges to master automatically released?

Once you've added the `Jenkinsfile.continuous` file, copied the
Jenkins jobs for your new repo, and added a webhook, Jenkins will
automatically build and publish your artifact when a pull request is
merged to master.

<!-- Reference-style links -->
[upbound-jenkins]: https://jenkinsci.upbound.io/job/crossplane/
