# Contributing Guide

**Want to contribute? Great!**
We try to make it easy, and all contributions, even the smaller ones, are more than welcome. This includes bug reports,
fixes, documentation, examples... But first, read this page (including the small print at the end).

* [Legal](#legal)
* [Reporting an issue](#reporting-an-issue)
* [Building](#building)
* [Before you contribute](#before-you-contribute)
    + [Code reviews](#code-reviews)
    + [Coding Guidelines](#coding-guidelines)
    + [Continuous Integration](#continuous-integration)
    + [Tests and documentation are not optional](#tests-and-documentation-are-not-optional)
* [Setup](#setup)
    + [IDE Config and Code Style](#ide-config-and-code-style)
        - [Eclipse Setup](#eclipse-setup)
        - [IDEA Setup](#idea-setup)
* [Building](#building)
* [Workflow tips](#workflow-tips)
* [Documentation](#documentation)
* [The small print](#the-small-print)

## Legal

All original contributions to Masters of Java are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0), version 2.0 or later, or, if another license is specified as governing the file or directory being modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/). The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Reporting an issue

This project uses GitHub issues to manage the issues. Open an issue directly in GitHub.

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and  what you would expect to see. Don't forget to indicate your Java and Maven version.

## Building

* Clone the repository: `git clone https://github.com/mastersofjava/mastersofjava.git`
* Navigate to the directory: `cd mastersofjava`
* Invoke `./mvnw -Dquickly` from the root directory

```bash
git clone https://github.com/mastersofjava/mastersofjava.git
cd mastersofjava
./mvnw -Dquickly
# Wait... success!
```

Building can take a few minutes depending on the hardware being used.

**NOTE:** This build skipped all the tests, documentation generation etc. and used the Maven
goals `clean verify` by default. For more details about `-Dquickly` have a look at the `quick-build` profile
in the `pom.xml`.

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

Also, make sure you have set up your Git authorship correctly:

```sh
git config --global user.name "Your Full Name"
git config --global user.email your.email@example.com
```

If you use different computers to contribute, please make sure the name is the same on all your computers.

We use this information to acknowledge your contributions in release announcements.

### Code reviews

All submissions, including submissions by project members, need to be reviewed by at least one Masters of Java committer before being merged.

[GitHub Pull Request Review Process](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/about-pull-request-reviews) is followed for every pull request.

### Coding Guidelines

* We decided to disallow `@author` tags in the Javadoc: they are hard to maintain, especially in a very active project,
  and we use the Git history to track authorship. GitHub also
  has [this nice page with your contributions](https://github.com/mastersofjava/mastersofjava/graphs/contributors).
* Commits should be atomic and semantic. Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.
  We use merge commits so the GitHub Merge button cannot do that for us. If you don't know how to do that, just ask in
  your pull request, we will be happy to help!

### Continuous Integration

Because we are all humans, and to ensure Masters of Java is stable for everyone, all changes must go through 
Masters of Java continuous integration. Masters of Java CI is based on GitHub Actions, which means that everyone has the ability to automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process, so that the contributor gets immediate feedback, while at the same time keeping our CI fast and healthy for everyone. The process requires only one additional step to enable Actions on your fork (clicking the green button in the actions
tab).

To keep the caching of non-Master of Java artifacts efficient (speeding up CI), you should occasionally sync the `main`
branch of your fork with `main` of this repo (e.g. monthly).

### Tests are not optional

Don't forget to include tests in your pull requests. Also don't forget the documentation, see [Documentation](#documentation).

## Setup

If you have not done so on this machine, you need to:

* Make sure you have a case-sensitive filesystem. Java development on a case-insensitive filesystem can cause headaches.
    * Linux: You're good to go.
    * macOS: Use the `Disk Utility.app` to check. It also allows you to create a case-sensitive volume to store your code projects. See this [blog entry](https://karnsonline.com/case-sensitive-apfs/) for more.
    * Windows: [Enable case sensitive file names per directory](https://learn.microsoft.com/en-us/windows/wsl/case-sensitivity)
* Install Git and configure your GitHub access
* Install Java SDK 17+ (OpenJDK recommended)

A container engine (such as [Docker](https://www.docker.com/) is not strictly
necessary: it is used to build the Masters of Java containers which is not enabled by default.

* For Docker, check [the installation guide](https://docs.docker.com/install/),
  and [the macOS installation guide](https://docs.docker.com/docker-for-mac/install/). If you just install docker, be
  sure that your current user can run a container (no root required). On Linux,
  check [the post-installation guide](https://docs.docker.com/install/linux/linux-postinstall/).

### IDE Config and Code Style

Masters of Java has a strictly enforced code style. Code formatting is done by the Eclipse code formatter, using the config
files found in the `ide` directory. By default, when you run `./mvnw verify`, the code will
be formatted automatically. When submitting a pull request the CI build will fail if running the formatter results in
any code changes, so it is recommended that you always run a full Maven build before submitting a pull request.

If you want to run the formatting without doing a full build, you can run `./mvnw process-sources`.

#### Eclipse Setup

##### Formatting

Open the *Preferences* window, and then navigate to _Java_ -> _Code Style_ -> _Formatter_. Click _Import_ and then
select the `eclipse-format.xml` file in the `ide` directory.

Next navigate to _Java_ -> _Code Style_ -> _Organize Imports_. Click _Import_ and select the `eclipse.importorder` file
in the `ide` directory.

#### IDEA Setup

##### Formatting

Open the _Preferences_ window (or _Settings_ depending on your edition), navigate to _Plugins_ and install
the [Adapter for Eclipse Code Formatter](https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter) from the
Marketplace.

Restart your IDE, open the *Preferences* (or *Settings*) window again and navigate to _Adapter for Eclipse Code
Formatter_ section on the left pane.

Select _Use Eclipse's Code Formatter_, then change the _Eclipse workspace/project folder or config file_ to point to the
`eclipse-format.xml` file in the `ide` directory. Make sure the _Optimize Imports_ box is
ticked. Then, select _Import Order from file_ and make it point to the `eclipse.importorder` file in the `ide` directory.

Next, disable wildcard imports:
navigate to _Editor_ -> _Code Style_ -> _Java_ -> _Imports_
and set _Class count to use import with '\*'_ to `999`. Do the same with _Names count to use static import with '\*'_.

## Contributing to Masters of Java

Obviously, when you contribute a change may impact any part of Masters of Java. Thus, it is recommended to use the
following approach:

* run `./mvnw clean verify` from the root directory to make sure you haven't broken anything obvious
* push your work to your own fork of Masters of Java to trigger CI there
* you can create a draft pull request to keep track of your work
* wait until the build is green in your fork (use your own judgement if it's not fully green) before marking your pull
  request as ready for review (which will trigger Masters of Java CI)

> **Note:** The `impsort-maven-plugin` and `formatter-maven-plugin` use the `.cache` directory on each module to speed up the build.
> Because we have configured the plugin to store in a versioned directory, you may notice over time that the `.cache` directory grows in size. You can safely delete the `.cache` directory in each module to reclaim the space.
> Running `./mvnw clean -Dclean-cache` automatically deletes that directory for you.

## Documentation
All documentation is currently found in the [README.md](README.md). When contributing keep it updated.

## The small print
This project is an open source project, please act responsibly, be nice, polite and enjoy! 

Many thanks to the [Quarkus Community](https://quarkus.io) for there excellent maven build, GitHub CI setup and contributing documentation after which we modeled everything for this project.