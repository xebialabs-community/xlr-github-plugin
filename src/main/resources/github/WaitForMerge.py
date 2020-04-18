from github.GithubClient import GithubClient
import base64, time
from github import GithubException

g_client = GithubClient(server)
g = g_client.get_github_client(locals())
repo = g_client.get_repo(g, organization, repositoryName)
pr_details = repo.get_pull(prid)
pr_merged = pr_details.merged
pr_state = pr_details.state
if pr_state == "closed":
    if pr_merged == True:
        task.setStatusLine("Success")
    elif pr_merged == False: # PR is closed without being merged
        raise Exception("[PR %s] was closed without merge " % (prid))
else:
    task.setStatusLine("Waiting for PR to be merged...")
    task.schedule("github/WaitForMerge.py", pollInterval)
