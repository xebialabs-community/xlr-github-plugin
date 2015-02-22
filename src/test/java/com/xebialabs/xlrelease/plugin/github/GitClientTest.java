/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import com.google.common.io.Files;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Date;

import static com.google.common.base.Charsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class GitClientTest {

    private static final PersonIdent JOHN_B = new PersonIdent("John B", "john-b@xebialabs.com");
    private static final PersonIdent JOHN_C = new PersonIdent("John C", "john-c@xebialabs.com");

    private static final String TARGET_BRANCH = "master";
    private static final String SOURCE_BRANCH = "REL-0001";

    private GitClient git;
    private File testFolder;
    private File testFile;
    private Git testRepo;

    @Before
    public void setup() throws Exception {
        testFolder = Files.createTempDir();
        testFile = new File(testFolder, "test.txt");
        Files.touch(testFile);

        System.out.println("Repo is: " + testFolder);

        testRepo = Git.init().setDirectory(testFolder).call();
        testRepo.add().addFilepattern(testFile.getName()).call();
        commitChange("init", JOHN_B, "Initial commit");

        testRepo.checkout().setCreateBranch(true).setName(SOURCE_BRANCH).call();

        git = spy(new GitClient("test-repo", "john", "b"));
        doReturn(testRepo).when(git).fetchOrCloneRepository(anyListOf(String.class));
        doNothing().when(git).push(any(Git.class));
        when(git.remote(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                String branch = (String) invocationOnMock.getArguments()[0];
                return R_HEADS + branch;
            }
        });
    }

    @After
    public void tearDown() {
        org.assertj.core.util.Files.delete(testFolder);
    }

    @Test
    public void should_merge_changes() throws Exception {
        commitChange("change 1", JOHN_B);
        commitChange("change 2", JOHN_B);

        String sha = git.squashBranch(SOURCE_BRANCH, TARGET_BRANCH, "I squashed it");

        RevCommit commit = checkout(TARGET_BRANCH);
        assertThat(commit.getFullMessage()).isEqualTo("I squashed it");
        assertThat(sha).isEqualTo(commit.getName());
        assertThat(Files.toString(testFile, UTF_8));
        int current = (int) (System.currentTimeMillis() / 1000);
        assertThat(commit.getCommitTime()).isBetween(current - 5, current + 5);
    }

    @Test(expected = MergeFailedException.class)
    public void should_fail_when_not_mergeable() throws Exception {
        commitChange("change 1", JOHN_B);
        checkout(TARGET_BRANCH);
        commitChange("change 2", JOHN_C);

        git.squashBranch(SOURCE_BRANCH, TARGET_BRANCH, "I squashed it");
    }

    @Test
    public void should_identify_main_author_on_single_commit() throws Exception {
        commitChange("change 1", JOHN_C);
        git.squashBranch(SOURCE_BRANCH, TARGET_BRANCH, "I squashed it");
        RevCommit commit = checkout(TARGET_BRANCH);

        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo(JOHN_C.getEmailAddress());
    }

    @Test
    public void should_identify_main_author_on_multiple_commits() throws Exception {
        commitChange("change 1", JOHN_B);
        commitChange("change 2", JOHN_C);
        commitChange("change 3", JOHN_C);
        commitChange("change 4", JOHN_B);
        commitChange("change 5", JOHN_C);
        git.squashBranch(SOURCE_BRANCH, TARGET_BRANCH, "I squashed it");
        RevCommit commit = checkout(TARGET_BRANCH);

        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo(JOHN_C.getEmailAddress());
    }

    @Test
    public void should_give_first_author_priority() throws Exception {
        commitChange("change 1", JOHN_C);
        commitChange("change 2", JOHN_B);
        commitChange("change 3", JOHN_C);
        commitChange("change 4", JOHN_B);
        git.squashBranch(SOURCE_BRANCH, TARGET_BRANCH, "I squashed it");
        RevCommit commit = checkout(TARGET_BRANCH);

        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo(JOHN_C.getEmailAddress());
    }

    private RevCommit checkout(String branch) throws GitAPIException {
        testRepo.checkout().setName(branch).call();
        return testRepo.log().setMaxCount(1).call().iterator().next();
    }

    private void commitChange(String text, PersonIdent author) throws Exception {
        commitChange(text, author, "Change by " + author.getName());
    }

    private void commitChange(String text, PersonIdent author, String message) throws Exception {
        Files.write(text, testFile, UTF_8);
        testRepo.add().addFilepattern(testFile.getName()).call();
        testRepo.commit()
                .setMessage(message)
                .setCommitter(new PersonIdent(author, new Date()))
                .call();
    }

}
