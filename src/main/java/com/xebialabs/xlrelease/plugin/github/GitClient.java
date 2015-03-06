/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import static java.lang.String.format;
import static org.eclipse.jgit.api.ResetCommand.ResetType.HARD;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.FETCH_HEAD;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

public class GitClient {

    private static final Logger logger = LoggerFactory.getLogger(GitClient.class);

    private static final Config config = ConfigFactory.load("xl-release.conf").withFallback(ConfigFactory.load());

    private String repositoryUrl;
    private String username;
    private String password;
    private File repositoriesFolder;

    public GitClient(String repositoryUrl, String username, String password) {
        this.repositoryUrl = repositoryUrl;
        this.username = username;
        this.password = password;
        this.repositoriesFolder = new File(config.getString("xlr-github-plugin.repositories-location"));
    }

    /**
     * Merges one branch into another, squashing all commits into one with a given message.
     * @param sourceBranch  branch to merge
     * @param targetBranch  base branch
     * @param message       message for the new commit
     * @return              ID of the created squash commit
     */
    public String squashBranch(final String sourceBranch, final String targetBranch, final String message) throws GitAPIException, IOException, PrivilegedActionException {
        // Do in privileged block to avoid problems when this is invoked from a script task
        return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws GitAPIException, IOException {
                return doSquashBranch(sourceBranch, targetBranch, message);
            }
        });
    }

    private String doSquashBranch(String sourceBranch, String targetBranch, String message) throws GitAPIException, IOException {
        Git git = fetchOrCloneRepository(Arrays.asList(targetBranch, sourceBranch));

        if (git.getRepository().getRef(targetBranch) == null) {
            // Initial checkout
            git.checkout()
                    .setName(targetBranch)
                    .setCreateBranch(true)
                    .setStartPoint(DEFAULT_REMOTE_NAME + "/" + targetBranch)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .setForce(true)
                    .call();
        } else {
            // Cleanup and pull target branch
            git.reset()
                    .setMode(HARD)
                    .setRef(HEAD)
                    .call();
            git.checkout()
                    .setName(targetBranch)
                    .setForce(true)
                    .call();
            git.merge()
                    .include(git.getRepository().getRef(FETCH_HEAD))
                    .call();
        }

        List<RevCommit> newCommits = getNewCommits(git, remote(sourceBranch), remote(targetBranch));
        logger.info("Found {} new commits in branch {}", newCommits.size(), sourceBranch);
        if (logger.isDebugEnabled()) {
            for (RevCommit rev : newCommits) {
                logger.debug("[{}] {}: {}", new Date(1000L * rev.getCommitTime()),
                        rev.getAuthorIdent().getEmailAddress(), rev.getFullMessage().replace('\n', ' '));
            }
        }
        PersonIdent mainAuthor = getMainAuthor(newCommits);

        MergeResult mergeResult = git.merge()
                .include(git.getRepository().getRef(remote(sourceBranch)))
                .setStrategy(MergeStrategy.RECURSIVE)
                .setSquash(true)
                .call();
        if (!mergeResult.getMergeStatus().isSuccessful()) {
            throw new MergeFailedException(format("Could not squash-merge branch %s into %s: %s",
                    sourceBranch, targetBranch, mergeResult));
        }

        logger.info("Squash-merging branch {} into {} with author {} and message [{}]",
                sourceBranch, targetBranch, mainAuthor.getEmailAddress(), message);
        RevCommit squashCommit = git.commit()
                .setMessage(message)
                .setCommitter(new PersonIdent(mainAuthor, new Date()))
                .call();
        push(git);
        return squashCommit.getName();
    }

    @VisibleForTesting
    protected void push(Git git) throws GitAPIException {
        git.push().setCredentialsProvider(getCredentials()).call();
    }

    @VisibleForTesting
    protected String remote(String branch) {
        return R_REMOTES + DEFAULT_REMOTE_NAME + "/" + branch;
    }

    @VisibleForTesting
    protected Git fetchOrCloneRepository(List<String> branches) {

        List<String> branchSpecs = Lists.transform(branches, new Function<String, String>() {
            @Override
            public String apply(String branch) {
                return R_HEADS + branch;
            }
        });
        List<RefSpec> refSpecs = Lists.transform(branches, new Function<String, RefSpec>() {
            @Override
            public RefSpec apply(String branch) {
                return new RefSpec(format("+%s%s:%s%s/%s", R_HEADS, branch, R_REMOTES, DEFAULT_REMOTE_NAME, branch));
            }
        });

        String rootPath = repositoryUrl.replaceAll("\\W", "_");
        File repoFolder = new File(repositoriesFolder, rootPath);
        if (repoFolder.exists()) {
            logger.debug("Repository [{}] is checked out to {}, fetching", repositoryUrl, repoFolder);
            try {
                Git repo = Git.open(repoFolder);
                repo.fetch()
                        .setRefSpecs(refSpecs)
                        .setCredentialsProvider(getCredentials())
                        .call();
                return repo;
            } catch (Exception e) {
                throw new RuntimeException(format("Could not initialize repository %s at %s: %s",
                        repositoryUrl, repoFolder, e.getMessage()), e);
            }

        } else {
            logger.debug("Repository [{}] is not checked out, cloning", repositoryUrl, repoFolder);
            if (!repositoriesFolder.exists() && !repositoriesFolder.mkdirs()) {
                throw new IllegalStateException("Could not create repositories folder " + repositoriesFolder);
            }
            try {
                return Git.cloneRepository()
                        .setDirectory(repoFolder)
                        .setURI(repositoryUrl)
                        .setBranchesToClone(branchSpecs)
                        .setCredentialsProvider(getCredentials())
                        .setBranch(branches.get(0))
                        .call();
            } catch (GitAPIException e) {
                throw new RuntimeException("Could not checkout repository " + repositoryUrl, e);
            }
        }

    }

    private List<RevCommit> getNewCommits(Git git, String branchRef, String baseRef) throws IOException, GitAPIException {
        Iterable<RevCommit> baseLogs = git.log()
                .add(git.getRepository().resolve(baseRef))
                .call();
        Set<String> baseCommits = new HashSet<>();
        for (RevCommit log : baseLogs) {
            baseCommits.add(log.getName());
        }

        Iterable<RevCommit> branchLogs = git.log()
                .add(git.getRepository().resolve(branchRef))
                .call();
        List<RevCommit> newCommits = new ArrayList<>();
        for (RevCommit log : branchLogs) {
            if (!baseCommits.contains(log.getName())) {
                newCommits.add(log);
            } else {
                break;
            }
        }
        return Lists.reverse(newCommits);
    }

    private PersonIdent getMainAuthor(List<RevCommit> commits) {
        if (commits == null || commits.isEmpty()) {
            return null;
        }
        final LinkedHashMap<PersonIdent, Integer> authorToCount = new LinkedHashMap<>();

        for (RevCommit commit : commits) {
            // Ignore the commit time
            PersonIdent author = new PersonIdent(commit.getAuthorIdent(), 0L, commit.getAuthorIdent().getTimeZoneOffset());
            int count = authorToCount.containsKey(author) ? authorToCount.get(author) : 0;
            authorToCount.put(author, count + 1);
        }
        TreeSet<PersonIdent> sorted = new TreeSet<>(new Comparator<PersonIdent>() {
            @Override
            public int compare(PersonIdent p1, PersonIdent p2) {
                return authorToCount.get(p2) - authorToCount.get(p1);
            }
        });
        for (Map.Entry<PersonIdent, Integer> ac : authorToCount.entrySet()) {
            sorted.add(ac.getKey());
        }
        return sorted.first();
    }

    private UsernamePasswordCredentialsProvider getCredentials() {
        return new UsernamePasswordCredentialsProvider(username, password);
    }
}
