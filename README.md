# XL Release GitHub Plugin #

The `xlr-github-plugin` is an [XL Release](https://docs.xebialabs.com/xl-release/index.html) plugin that allows you to integrate with GitHub.

**NOTE**: this plugin depends on the `xlr-git-plugin` which is shipped in standard XL Release distribution.


## Overview ##

This plugin adds a library and several helper methods which let you work with GitHub API using Script tasks. Additionally it defines a web hook which can be used to automatically squash commits of a pull request.

### Usage of GitHub API ###

You can use GitHub API using Script tasks in your XL Release template. For example, to create a comment on an issue or a pull request you can use following script:

    pull_request_number = 2
    repository_full_name = 'xebialabs-community/xlr-github-plugin'
    comment = 'Hello, World!'

    from com.xebialabs.xlrelease.plugin.github import GitHubUtils
    from org.eclipse.egit.github.core.service import IssueService
    github_client = GitHubUtils.getGitHubClient(repository_full_name)
    repository = GitHubUtils.createRepositoryId(repository_full_name)
    issue_service = IssueService(github_client)

    issue_service.createComment(repository, pull_request_number, comment)

Please check the [GitHub API](https://developer.github.com/v3/) and [Eclipse GitHub Java API documentation](https://github.com/eclipse/egit-github/blob/master/org.eclipse.egit.github.core/README.md) for the list of services and methods available.

### Squash pull request hook ###

With this plugin you can configure your XL Release and GitHub repository to automatically squash commits before merging them into base branch.

*Why would you need this?* At [XebiaLabs](http://www.xebialabs.com) we develop using feature branches: a developing pair implements a feature in a branch and creates a pull request when it is ready. Another pair tests and reviews the code and then merges the feature into master branch. However before merging we squash all commits into one. This way it's easier to manage a feature: for example, we then can easily cherry-pick it into maintenance branch to be released in maintenance versions of our products.

GitHub does not support this kind of feature out of the box, so we decided to automate it. With this plugin you can type "*XL Release: squash this*" as a comment to a pull request, and then it will:

1. Squash all commits of the pull request into one and push it into the head branch;
2. Add a comment "Squashed by commit abc123" to the pull request;
3. Close the pull request;
4. Delete the feature branch.

Some details of the feature:

* Your XL Release instance must be available from the internet, otherwise GitHub won't be able to notify it about a new comment.
* The committer who made the most of commits in the pull request is selected as the author of squashed commit.
* It only works if both `head` and `base` of the pull request are in the same repository. It does not work for pull requests from forked repositories, for security reasons.


## Building ##

To build this project you need an unpacked instance of XL Release server on your development machine. You have to either specify it in your `~/.gradle/gradle.properties`:

    xlReleaseHome=/path/to/xl/release/server

or when running a gradle task: `./gradlew build -PxlReleaseHome=/path/to/xl/release/server`


## Installing ##

To install this plugin you need to put two jar files into `XL_RELEASE_SERVER_HOME/plugins` and restart XL Release:

* `xlr-github-plugin-<version>.jar` (you can find it in `xlr-github-plugin/build/libs/` once you build it),
* [`org.eclipse.egit.github.core-2.1.5.jar`](http://central.maven.org/maven2/org/eclipse/mylyn/github/org.eclipse.egit.github.core/2.1.5/org.eclipse.egit.github.core-2.1.5.jar).


## Configuring squash pull request hook ##

To configure the pull request squashing you first need to add the web hook in your GitHub repository (or repositories).

1. Go to https://github.com/<user-or-organization>/<repository>/settings/hooks/new
2. Paste the payload URL: `http(s)://<username>:<password>@<your-xl-release-instance-host>:<port>/<xl-release-context>/api/extension/github/pr-merge-hook`

3. Select *Let me select individual events* and choose only *Issue comment*.

Then you need to configure a "Git Repository" in XL Release. Go to _Settings_ -> _Configuration_ -> _Git: Repository_ -> _Add Repository_ and specify your GitHub repository details, for example:

* Name: xlr-github-plugin
* URL: https://github.com/xebialabs-community/xlr-github-plugin.git
* Username: (your username)
* Password: (your password)

Lastly you need to configure the XL Release template which will do the squashing. To have it created you need to do a first trigger of the web hook: create a test pull request and type a comment: *XL Release: bootstrap*. Then go to XL Release templates screen and filter templates by *github* keyword. Find the newly created template, open it, go to *Properties* and set the proper *Run scripts as* username and password.

Now you can test it. Type a comment on your test pull request: *XL Release: squash this*. In several seconds you should see another comment popping up with an ID of squashed commit, and the pull request becomes closed.

To debug what's happening in XL Release you can go there to *Releases*, filter by *Completed* and see which tasks were executed to merge your pull request and how long it took.


# Development #

## Deploying ##

This plugin makes use of [Gradle XL Deploy plugin](https://github.com/xebialabs-community/gradle-xld-plugin) so you can easily deploy it to your XL Release instance. The deployment details are specified in `gradle.properties` file: by default it uses an XL Deploy instance running on localhost, but you can override these values in your `~/.gradle/gradle.properties`.