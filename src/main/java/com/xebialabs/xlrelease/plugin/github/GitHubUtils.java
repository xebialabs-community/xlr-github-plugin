/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import com.xebialabs.xlrelease.domain.configuration.HttpConnection;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class which can be used to find a configured Git repository
 * in XL Release and initialize GitHub client from that.
 */
public class GitHubUtils {

    public static GitHubClient getGitHubClient(String repositoryFullName) {
        HttpConnection repo = getConfiguredGitRepository(repositoryFullName);
        return new GitHubClient().setCredentials(repo.getUsername(), repo.getPassword());
    }

    public static GitClient getGitClient(String repositoryFullName) {
        HttpConnection repo = getConfiguredGitRepository(repositoryFullName);
        return new GitClient(repo.getUrl(), repo.getUsername(), repo.getPassword());
    }

    public static HttpConnection getConfiguredGitRepository(String repositoryFullName) {
        Map<String, String> params = new HashMap<>();
        params.put("url", String.format("https://github.com/%s.git", repositoryFullName));
        params.put("username", "xlr-git-plugin");
        params.put("password", "password");
        return new HttpConnection(params);
    }

    public static RepositoryId createRepositoryId(String repositoryFullName) {
        String[] usernameAndRepo = repositoryFullName.split("/");
        return new RepositoryId(usernameAndRepo[0], usernameAndRepo[1]);
    }

}
