#
# Copyright 2020 XEBIALABS
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

from github.GithubClient import GithubClient
import org.slf4j.LoggerFactory as LoggerFactory

logger = LoggerFactory.getLogger("Github")

g_client = GithubClient(server)
# Call method get_github_client in GithubClient.py
g = g_client.get_github_client()
# Now that we have our github client, we can retrieve the user information
user_login = g.get_user().login
logger.debug("my user login value is: %s" % user_login)
user_id = g.get_user().id
logger.debug("my user id value is: %s" % user_id)
user_name = g.get_user().name
logger.debug("my user name value is: %s" % user_name)

# Set the response data attribute with the JSON object I want to pass to the html for display
data = {"user_login": user_login,
        "user_id": user_id,
        "user_name": user_name}
