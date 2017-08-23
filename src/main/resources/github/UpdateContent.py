#
# Copyright 2017 XEBIALABS
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
