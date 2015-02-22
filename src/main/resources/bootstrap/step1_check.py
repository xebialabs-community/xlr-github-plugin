import sys
from com.xebialabs.xlrelease.plugin.github import GitHubUtils
from org.eclipse.egit.github.core.service import PullRequestService
from org.eclipse.egit.github.core.service import IssueService

pull_request_number = int('${pull_request_number}')
pull_request_title = '${pull_request_title}'
repository_full_name = '${repository_full_name}'
pull_request_comment = '${pull_request_comment}'

github_client = GitHubUtils.getGitHubClient(repository_full_name)
repository = GitHubUtils.createRepositoryId(repository_full_name)
issue_service = IssueService(github_client)
pr_service = PullRequestService(github_client)


def cancel_release(message):
    logger.info('Creating a comment on PR #%s: "%s"' % (pull_request_number, message))
    issue_service.createComment(repository, pull_request_number, message)
    sys.exit(1)

if 'squash' not in pull_request_comment:
    cancel_release("I don't understand what you ask me. Did you mean 'xlr: squash this'?")

pr = pr_service.getPullRequest(repository, pull_request_number)
if not pr.isMergeable:
    cancel_release("This pull request is not automatically mergeable")

if pr.getBase().getRepo().getId() != pr.getHead().getRepo().getId():
    cancel_release("I can only merge pull requests from the same repository")
