/**
 * Copyright 2017 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
