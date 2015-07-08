/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_CONTENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;

/**
 * Extension of {@link org.eclipse.egit.github.core.service.ContentsService} which
 * also support updating of GitHub files.
 */
public class ContentsService extends org.eclipse.egit.github.core.service.ContentsService {

    public ContentsService() {
        super();
    }

    public ContentsService(final GitHubClient client) {
        super(client);
    }

    /**
     * Update contents at path in the given repository on master branch
     *
     * @param repository    GitHub repository
     * @param newContents   Specification of new content with base64-encoded content, sha and path
     * @param message       Commit message
     * @return commit in which the content was updated
     * @throws IOException
     */
    public Commit updateContents(
            IRepositoryIdProvider repository, RepositoryContents newContents, String message)
            throws IOException {
        return updateContents(repository, newContents, message, null);
    }

    /**
     * Update contents at path in the given repository on master branch
     *
     * @param repository    GitHub repository
     * @param newContents   Specification of new content with base64-encoded content, sha and path
     * @param message       Commit message
     * @param ref           Branch specification
     * @return commit in which the content was updated
     * @throws IOException
     */
    public Commit updateContents(
            IRepositoryIdProvider repository, RepositoryContents newContents, String message, String ref)
            throws IOException {
        String id = getId(repository);

        StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
        uri.append('/').append(id);
        uri.append(SEGMENT_CONTENTS);
        String path = newContents.getPath();
        if (path != null && path.length() > 0) {
            if (path.charAt(0) != '/')
                uri.append('/');
            uri.append(path);
        }
        GitHubRequest request = createRequest();
        request.setUri(uri);
        request.setType(Commit.class);

        Map<String, String> params = new HashMap<>();
        params.put("path", newContents.getPath());
        params.put("message", message);
        params.put("content", newContents.getContent());
        params.put("sha", newContents.getSha());
        if (ref != null && ref.length() > 0)
            params.put("branch", ref);

        request.setParams(params);

        RepositoryCommit commit = client.put(uri.toString(), params, RepositoryCommit.class);
        return commit.getCommit();
    }

}
