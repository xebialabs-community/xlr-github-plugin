#
# THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
# FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
#

from org.eclipse.egit.github.core import RepositoryId
from org.eclipse.egit.github.core.client import GitHubClient
from com.xebialabs.xlrelease.plugin.github import ContentsService
from org.eclipse.jgit.util import Base64
from java.lang import String
from java.util.regex import Pattern

github_client = GitHubClient().setCredentials(gitRepository['username'], gitRepository['password'])
repository_id = RepositoryId.createFromUrl(gitRepository['url'].replace('.git', ''))
contents_service = ContentsService(github_client)

contents_object = contents_service.getContents(repository_id, filePath, branch).get(0)
current_contents_bytes = Base64.decode(contents_object.getContent())
current_contents = String(current_contents_bytes, "UTF-8")

flags = Pattern.MULTILINE ^ Pattern.DOTALL
matcher = Pattern.compile(regex, flags).matcher(current_contents)
if matcher.find():
    new_contents = matcher.replaceAll(replacement)
    print "Replacing contents of %s/%s (%s) from:\n%s\nto:\n%s" % (repository_id, filePath, branch, current_contents, new_contents)
    contents_object.setContent(Base64.encodeBytes(String(new_contents).getBytes("UTF-8")))
    commit = contents_service.updateContents(repository_id, contents_object, commitMessage, branch)
    commitId = commit.getSha()
else:
    print "Did not find any occurrences of pattern [%s] in content:\n%s" % (regex, current_contents)
    commitId = None
