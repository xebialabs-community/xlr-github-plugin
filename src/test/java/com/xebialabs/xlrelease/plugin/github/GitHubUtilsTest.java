package com.xebialabs.xlrelease.plugin.github;

import org.junit.Test;

import static com.xebialabs.xlrelease.plugin.github.GitHubUtils.urlMatchesFullRepositoryName;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class GitHubUtilsTest {

    @Test
    public void should_find_repositories_with_correct_url() {
        String name = "xebialabs/xl-deploy";
        assertMatches("https://github.com/xebialabs/xl-deploy.git", name, true);
        assertMatches("https://github.com/xebialabs/xl-deploy", name, true);
        assertMatches("git@github.com:xebialabs/xl-deploy.git", name, true);

        assertMatches("https://github.com/xebialabs/xl-deploy-installer.git", name, false);
        assertMatches("https://github.com/xebialabs/xl-deploy-installer", name, false);
        assertMatches("https://github.com/xebialabs/xl-deploy_something", name, false);
    }

    private void assertMatches(String url, String repositoryFullName, boolean matches) {
        assertThat(urlMatchesFullRepositoryName(url, repositoryFullName), equalTo(matches));
    }

}