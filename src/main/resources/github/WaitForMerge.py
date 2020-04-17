from github.GithubClient import GithubClient
import base64, time
from github import GithubException

g_client = GithubClient(server)
g = g_client.get_github_client(locals())
repo = g_client.get_repo(g, organization, repositoryName)
# Call the function to check PR is merged
isMerged = False
while (not isMerged):
	pr_details = repo.get_pull(prid)
	pr_merged = pr_details.merged
	pr_state = pr_details.state
	if pr_state == "closed":
		if pr_merged == True:
			isMerged = True
			print "PR merged and closed"
		elif pr_merged == False: # PR is closed without being merged
			print "PR closed without merge"
			exit(1)
	else:
		time.sleep(pollInterval)
