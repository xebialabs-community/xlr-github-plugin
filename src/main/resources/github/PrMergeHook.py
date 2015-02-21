#
# THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
# FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
#

import sys
import re

from org.eclipse.egit.github.core.client import GsonUtils
from org.eclipse.egit.github.core.event import IssueCommentPayload
from org.eclipse.egit.github.core.client import GitHubClient
from org.eclipse.egit.github.core.service import PullRequestService
from org.eclipse.egit.github.core import RepositoryId
from org.eclipse.egit.github.core.service import IssueService
from com.google.gson import Gson
from com.xebialabs.xlrelease.plugin.git import GitClient


def handle_request(json_dict):
    try:
        json_string = Gson().toJson(json_dict)
        event = GsonUtils.fromJson(json_string, IssueCommentPayload)
    except StandardError:
        _, e, _ = sys.exc_info()
        print "Could not parse payload as an issue comment, check your GitHub Webhook configuration. " \
              "Error: %s. Payload:\n%s" % (e, json_string)
        return

    if not event.getIssue().getPullRequest() or not event.getIssue().getPullRequest().getUrl():
        # Comment is not on a pull request
        return

    if event.getIssue().getState() != IssueService.STATE_OPEN:
        # Pull request is already closed
        return

    message = event.getComment().getBody().lower()
    if not message.startswith("xlr:"):
        # Comment is not for XL Release
        return

    repo_full_name = re.search('.*github.com/repos/(.*)/pulls/.*', event.getIssue().getPullRequest().getUrl()).group(1)
    xlr_git_repo = _find_repository_in_xlr(repo_full_name)

    github_client = GitHubClient().setCredentials(xlr_git_repo['username'], xlr_git_repo['password'])
    repository = RepositoryId("xlr-git-plugin", "xlr-git-plugin")
    issue_service = IssueService(github_client)
    pr_service = PullRequestService(github_client)
    pr_number = event.getIssue().getNumber()

    if 'squash' not in message:
        _create_comment(issue_service, repository, pr_number,
                        "I don't understand what you ask me. Did you mean 'xlr: squash this'?")
        return

    pr = pr_service.getPullRequest(repository, pr_number)
    if not pr.isMergeable:
        _create_comment(issue_service, repository, pr_number,
                        "This pull request is not automatically mergeable")
        return

    if pr.getBase().getRepo().getId() != pr.getHead().getRepo().getId():
        _create_comment(issue_service, repository, pr_number,
                        "I can only merge pull requests from the same repository")
        return

    squash_commit_it = _squash(xlr_git_repo, pr)
    _create_comment(issue_service, repository, pr_number,
                    "Merged this pull request by squashed commit %s" % squash_commit_it)

    pr.setState(IssueService.STATE_CLOSED)
    pr = pr_service.editPullRequest(repository, pr)

    if pr.getHead().getRef() != 'master':
        github_client.delete('/repos/%s/git/refs/heads/%s' % (repo_full_name, pr.getHead().getRef()))


def _find_repository_in_xlr(repo_full_name):
    mock_repo = {
        'url': 'https://github.com/%s.git' % repo_full_name,
        'username': 'xlr-git-plugin',
        'password': 'password'
    }
    return mock_repo


def _create_comment(issue_service, repository, pr_number, message):
    issue_service.createComment(repository, pr_number, message)


def _squash(xlr_git_repo, pr):
    git_client = GitClient(xlr_git_repo['url'], xlr_git_repo['username'], xlr_git_repo['password'])
    source_branch = pr.getHead().getRef()
    target_branch = pr.getBase().getRef()
    merge_message = pr.getTitle()
    print "Merging branch %s into %s in repository %s with message '%s'" % (source_branch, target_branch, xlr_git_repo['url'], merge_message)
    return git_client.squashBranch(source_branch, target_branch, merge_message)


handle_request(request.entity)

# from com.xebialabs.xlrelease.api.v1.forms import StartRelease
# from java.util import HashMap
#
# prId = request.entity['number']
# comment = request.entity['comment']
# if comment and 'merge' in comment.lower():
# template = templateApi.getTemplates('Merge GitHub Pull Request')[0]
#     params = StartRelease()
#     params.setReleaseTitle('Merge GitHub PR #%s' % prId)
#     vars = HashMap()
#     vars.put('${prId}', '%s' % prId)
#     params.setReleaseVariables(vars)
#     startedRelease = templateApi.start(template.id, params)
#     response.entity = startedRelease
