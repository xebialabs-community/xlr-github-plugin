# XL Release GitHub Plugin #

The `xlr-github-plugin` is an [XL Release](https://docs.xebialabs.com/xl-release/index.html) plugin that allows you to integrate with GitHub.

**NOTE**: this plugin depends on the `xlr-git-plugin` which is shipped in standard XL Release distribution.


## Overview ##

This plugin adds a library and several helper methods which let you work with GitHub API using Script tasks.

### CI status ###

[![Build Status][xlr-github-plugin-travis-image] ][xlr-github-plugin-travis-url]
[![Github All Releases][xlr-github-plugin-downloads-image] ]()

[xlr-github-plugin-travis-image]: https://travis-ci.org/xebialabs-community/xlr-github-plugin.svg?branch=master
[xlr-github-plugin-travis-url]: https://travis-ci.org/xebialabs-community/xlr-github-plugin
[xlr-github-plugin-downloads-image]: https://img.shields.io/github/downloads/xebialabs-community/xlr-github-plugin/total.svg

 

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

## Building ##

To build this project you need an unpacked instance of XL Release server on your development machine. You have to either specify it in your `~/.gradle/gradle.properties`:

    xlReleaseHome=/path/to/xl/release/server

or when running a gradle task: `./gradlew build -PxlReleaseHome=/path/to/xl/release/server`


## Installing ##

To install this plugin you need to put two jar files into `XL_RELEASE_SERVER_HOME/plugins` and restart XL Release:

* `xlr-github-plugin-<version>.jar` (you can find it in `xlr-github-plugin/build/libs/` once you build it),
* [`org.eclipse.egit.github.core-2.1.5.jar`](http://central.maven.org/maven2/org/eclipse/mylyn/github/org.eclipse.egit.github.core/2.1.5/org.eclipse.egit.github.core-2.1.5.jar).


# Development #

## Deploying ##

This plugin makes use of [Gradle XL Deploy plugin](https://github.com/xebialabs-community/gradle-xld-plugin) so you can easily deploy it to your XL Release instance. The deployment details are specified in `gradle.properties` file: by default it uses an XL Deploy instance running on localhost, but you can override these values in your `~/.gradle/gradle.properties`.

## Releasing ##

This project uses the [nebula-release-plugin](https://github.com/nebula-plugins/nebula-release-plugin), which in turn uses [gradle-git plugin](https://github.com/ajoberstar/gradle-git). So you can release a new version if this project using following commands:

* to release a new patch (default): `./gradlew final -Prelease.scope=patch`
* to release a new minor release: `./gradlew final -Prelease.scope=minor`
* to release a new major release: `./gradlew final -Prelease.scope=major`

By default when you build the project it builds a snapshot version of next (to be released) minor release. You can get rid of `-SNAPSHOT` in the version by adding command-line parameter `-Prelease.stage=final`. Note that your Git project must be clean to be able to set version to `final` stage.
