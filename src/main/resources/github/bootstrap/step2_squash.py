from com.xebialabs.xlrelease.plugin.github import GitHubUtils
from org.eclipse.egit.github.core.service import PullRequestService
from org.eclipse.egit.github.core.service import IssueService

pull_request_number = int('${pull_request_number}')
repository_full_name = '${repository_full_name}'

github_client = GitHubUtils.getGitHubClient(repository_full_name)
repository = GitHubUtils.createRepositoryId(repository_full_name)
issue_service = IssueService(github_client)
pr_service = PullRequestService(github_client)

pr = pr_service.getPullRequest(repository, pull_request_number)
git_client = GitHubUtils.getGitClient(repository_full_name)
source_branch = pr.getHead().getRef()
target_branch = pr.getBase().getRef()
merge_message = pr.getTitle()

logger.info("Merging branch %s into %s in repository %s with message '%s'" %
            (source_branch, target_branch, repository_full_name, merge_message))
squash_commit_id = git_client.squashBranch(source_branch, target_branch, merge_message)

issue_service.createComment(repository, pull_request_number,
                            "Merged this pull request by squashed commit %s" % squash_commit_id)
