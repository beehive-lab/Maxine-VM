# Contributing to Maxine

Maxine is an open-source project, and we invite researchers and developers to make contributions in the form of bug fixes, performance enhancements, features, documentation, and so forth.

To push your code upstream use pull requests on GitHub.
However, note that we are doing most development in a private git tree and we are working on a number of features which are not quite ready for public release.
Therefore, we would strongly encourage you to get in touch before starting to work on anything large, to avoid duplication of effort.
We can probably expedite our release of any work-in-progress (WIP) features you might be interested in, if you do that

Contributors should adopt the following process once they think their changes are ready to push:

1. Merge the latest changes from the Maxine master branch.
2. Test their code.
3. Make sure the code complies to the coding style of Maxine VM using `mx checkstyle` and passes `mx gate`.
4. Commit their code changed following the [git commit template](git/commit-template)
5. Open a new pull request where they explain their contribution and how it works in case of complex implementations.

The changes will then be reviewed and tested.
In case there are no errors, the contribution will be included in the head repository.
In any other case the contributors will be asked to resolve any issues and update their code.

## Reporting Bugs

If you think you have found a bug please open a new issue.
However, note that we have limited time available to investigate and fix bugs which are not affecting the workloads we are using.
Therefore, if you can't pinpoint the cause of the bug yourself, we ask that you provide as many details on how to reproduce it, and preferably provide a statically linked executable which triggers it.

