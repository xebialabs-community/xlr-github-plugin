from com.xebialabs.xlrelease.plugin.github import GitHubUtils
from org.eclipse.egit.github.core.service import PullRequestService

pull_request_number = int('${pull_request_number}')
repository_full_name = '${repository_full_name}'

github_client = GitHubUtils.getGitHubClient(repository_full_name)
repository = GitHubUtils.createRepositoryId(repository_full_name)
pr_service = PullRequestService(github_client)

pr = pr_service.getPullRequest(repository, pull_request_number)

if pr.getHead().getRef() != 'master':
    github_client.delete('/repos/%s/git/refs/heads/%s' % (repository_full_name, pr.getHead().getRef()))
else:
    logger.info("Don't ask me to delete 'master' branch!")