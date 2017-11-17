#
# Copyright 2017 XEBIALABS
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

from java.util.regex import Pattern
from xlr.github.GithubClient import GithubClient

g_client = GithubClient(server)
g = g_client.get_github_client(locals())


if organization:
    repo = g.get_organization(organization).get_repo(repositorName)
else:
    repo = g.get_user().get_repo(repositoryName)

file_contents = repo.get_file_contents(filePath, ref="refs/heads/%s" % branch)
current_contents = file_contents.content

flags = Pattern.MULTILINE ^ Pattern.DOTALL
matcher = Pattern.compile(regex, flags).matcher(current_contents)
if matcher.find():
    new_contents = matcher.replaceAll(replacement)
    print "Replacing contents of %s/%s (%s) from:\n%s\nto:\n%s" % (repositoryName, filePath, branch, current_contents, new_contents)
    result = repo.update_file(filePath, commitMessage, new_contents, file_contents.sha, branch)
    commitId = result['commit'].sha
else:
    print "Did not find any occurrences of pattern [%s] in content:\n%s" % (regex, current_contents)
    commitId = None
