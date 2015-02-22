/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.deployit.repository.RepositoryService;
import com.xebialabs.deployit.repository.SearchParameters;
import com.xebialabs.xlrelease.api.XLReleaseServiceHolder;
import com.xebialabs.xlrelease.domain.configuration.HttpConnection;
import com.xebialabs.xlrelease.views.ImportResult;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.filter;
import static java.lang.String.format;

/**
 * Utility class which can be used to find a configured Git repository
 * in XL Release and initialize GitHub client from that.
 */
public class GitHubUtils {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUtils.class);

    public static GitHubClient getGitHubClient(String repositoryFullName) {
        HttpConnection repo = getConfiguredGitRepository(repositoryFullName);
        return new GitHubClient().setCredentials(repo.getUsername(), repo.getPassword());
    }

    public static GitClient getGitClient(String repositoryFullName) {
        HttpConnection repo = getConfiguredGitRepository(repositoryFullName);
        return new GitClient(repo.getUrl(), repo.getUsername(), repo.getPassword());
    }

    public static HttpConnection getConfiguredGitRepository(final String repositoryFullName) {
        RepositoryService repositoryService = XLReleaseServiceHolder.getRepositoryService();
        List<ConfigurationItem> gitRepositories = repositoryService.listEntities(new SearchParameters()
                .setType(Type.valueOf("git.Repository"))
                .setPage(0)
                .setResultsPerPage(Integer.MAX_VALUE));
        Collection<ConfigurationItem> matching = filter(gitRepositories, new Predicate<ConfigurationItem>() {
            @Override
            public boolean apply(ConfigurationItem gitRepository) {
                String url = gitRepository.getProperty("url");
                return url != null && url.matches(".*[\\W]" + repositoryFullName + "($|\\W.*)");
            }
        });
        if (matching.isEmpty()) {
            throw new RuntimeException(format("Could not find git repository for '%s', " +
                    "please configure it in XL Release", repositoryFullName));
        }
        if (matching.size() > 1) {
            logger.warn(format("More than one git repository found matching '%s', using the first one",
                    repositoryFullName));
        }
        return (HttpConnection) matching.iterator().next();
    }

    public static RepositoryId createRepositoryId(String repositoryFullName) {
        String[] usernameAndRepo = repositoryFullName.split("/");
        return new RepositoryId(usernameAndRepo[0], usernameAndRepo[1]);
    }

    public static String bootstrapPRMergerTemplate() throws IOException {
        String json = getStringResource("/github/bootstrap/github-pr-merger-template.json");
        Gson gson = new Gson();
        for (String step : new String[] {
                "step1_check.py",
                "step2_squash.py",
                "step3_close_pr.py",
                "step4_delete_branch.py"}) {
            String stepCode = getStringResource("/github/bootstrap/" + step);
            json = json.replace(format("${%s}", step), gson.toJson(stepCode));
        }

        List<ImportResult> results = XLReleaseServiceHolder.getTemplateApi().importTemplate(json);
        return results.get(0).getId();
    }

    private static String getStringResource(String resource) throws IOException {
        URL resourceUrl = GitHubUtils.class.getResource(resource);
        if (resourceUrl == null) {
            throw new RuntimeException(format("Could not find %s in classpath", resource));
        }
        return Resources.toString(resourceUrl, Charsets.UTF_8);
    }

}
