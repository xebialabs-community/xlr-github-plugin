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
