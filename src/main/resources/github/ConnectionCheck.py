from github.GithubClient import GithubClient

g_client = GithubClient(configuration)
g = g_client.get_github_client()

# Then play with your Github objects:
g.get_repos()
