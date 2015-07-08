/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PrMergeHookScriptTest extends BaseXLReleaseTestServerTest {

  describe("PR merge hook endpoint script") {

    it("should fail on invalid json") {
      val scriptService = server.getScriptTestService
      val badJson = "{\"action\":{\"invalid\":\"object\"}}"
      val result = scriptService.executeEndpointScript("github/pr_merge_hook.py", null, badJson, "john")

      result.exception shouldBe None
      result.stderr should include (badJson)
      result.stderr.toLowerCase should include ("string")

      result.statusCode shouldBe Some(400)
    }

  }

}
