/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import org.junit.Test;

import com.xebialabs.xlplatform.endpoints.actors.ScriptDone;
import com.xebialabs.xlrelease.test.script.ScriptTestService;

import static org.assertj.core.api.Assertions.assertThat;

public class PrMergeHookScriptTest extends BaseXLReleaseTestServerTest {

    @Test
    public void should_fail_on_invalid_json() {
        ScriptTestService scriptService = server.getScriptTestService();
        String badJson = "{\"action\":{\"invalid\":\"object\"}}";
        ScriptDone result = scriptService.executeEndpointScript("github/pr_merge_hook.py", null, badJson, "john");

        assertThat(result.exception().isEmpty());
        assertThat(result.stderr()).contains(badJson);
        assertThat(result.stderr()).containsIgnoringCase("String");

        assertThat(result.statusCode().isDefined());
        assertThat(result.statusCode().get()).isEqualTo(400);
    }

}
