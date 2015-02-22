#
# THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
# FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
#

import sys
import re

from org.eclipse.egit.github.core.client import GsonUtils
from org.eclipse.egit.github.core.event import IssueCommentPayload
from org.eclipse.egit.github.core.service import IssueService
from com.google.gson import Gson
from com.xebialabs.xlrelease.api.v1.forms import StartRelease
from java.util import HashMap
from com.xebialabs.xlrelease.plugin.github import GitHubUtils


def handle_request(json_dict):
    try:
        json_string = Gson().toJson(json_dict)
        event = GsonUtils.fromJson(json_string, IssueCommentPayload)
    except StandardError:
        _, e, _ = sys.exc_info()
        logger.warn("Could not parse payload as an issue comment, check your GitHub Webhook "
                    "configuration. Error: %s. Payload:\n%s" % (e, json_string))
        return

    if not event.getIssue().getPullRequest() or not event.getIssue().getPullRequest().getUrl():
        logger.debug("Issue #%s is not a pull request" % event.getIssue().getNumber())
        return

    if event.getIssue().getState() != IssueService.STATE_OPEN:
        logger.debug("Pull request #%s is already closed" % event.getIssue().getNumber())
        return

    message = event.getComment().getBody().lower()
    if not _is_xlr_command(message):
        logger.debug('Comment is not an XL Release command: "%s"' % message)
        return

    repo_full_name = re.search('.*github.com/repos/(.*)/pulls/.*', event.getIssue().getPullRequest().getUrl()).group(1)
    pr_number = event.getIssue().getNumber()
    pr_title = event.getIssue().getTitle()

    start_pr_release(repo_full_name, pr_number, pr_title, message)


def start_pr_release(repo_full_name, pr_number, pr_title, comment):
    tag = 'pull_request_merger'
    pr_templates = templateApi.getTemplates(tag)
    if not pr_templates:
        template_id = GitHubUtils.bootstrapPRMergerTemplate()
    else:
        if len(pr_templates) > 1:
            logger.warn("Found more than one template with tag '%s', using the first one" % tag)
        template_id = pr_templates[0].id

    params = StartRelease()
    params.setReleaseTitle('Merge PR #%s: %s' % (pr_number, pr_title))
    variables = HashMap()
    variables.put('${pull_request_number}', '%s' % pr_number)
    variables.put('${pull_request_title}', '%s' % pr_title)
    variables.put('${repository_full_name}', '%s' % repo_full_name)
    variables.put('${pull_request_comment}', '%s' % comment)
    params.setReleaseVariables(variables)
    started_release = templateApi.start(template_id, params)
    response.entity = started_release
    logger.info("Started release %s to merge pull request %s" % (started_release.getId(), pr_number))


def _is_xlr_command(message):
    return message.strip().startswith("xlr") or message.strip().startswith("xl release")


handle_request(request.entity)
