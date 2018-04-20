#
# Copyright 2017 XEBIALABS
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

from github import Github


class GithubClient(object):
    def __init__(self, server):
        self.login_or_token = server["loginOrToken"]
        self.pwd = server["password"]
        self.client_id = server["clientId"]
        self.client_secret = server["clientSecret"]
        self.base_url = server["baseUrl"]
        self.timeout = server["timeout"]
        self.user_agent = server["userAgent"]
        self.per_page = server["perPage"]
        self.api_preview = server["apiPreview"]

    def get_github_client(self, variables=None):
        if variables:
            if variables['loginOrToken']:
                self.login_or_token = variables['loginOrToken']
            if variables['password']:
                self.pwd = variables['password']
            if variables['clientId']:
                self.client_id = variables['clientId']
            if variables['clientSecret']:
                self.client_secret = variables['clientSecret']

        # First create a Github instance:
        return Github(self.login_or_token, self.pwd, self.base_url, self.timeout, self.client_id, self.client_secret,
                      self.user_agent, self.per_page, self.api_preview)

    def get_repo(self, g_client, organization, repo_name):
        if organization:
            return g_client.get_organization(organization).get_repo(repo_name)
        return g_client.get_user().get_repo(repo_name)

    def get_tags(self, g_client, organization, repo_name):
        if organization:
            return self.get_repo(g_client, organization, repo_name).get_tags()
        return g_client.get_user().get_repo(repo_name).get_tags()
