/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github

import com.xebialabs.xlrelease.XLReleaseIntegrationScalaTest
import com.xebialabs.xlrelease.script.ScriptTestService
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.springframework.beans.factory.annotation.Autowired

@RunWith(classOf[JUnitRunner])
class PrMergeHookScriptTest extends XLReleaseIntegrationScalaTest {

  @Autowired
  private var scriptTestService: ScriptTestService = _

  describe("PR merge hook endpoint script") {

    it("should fail on invalid json") {
      val badJson = "{\"action\":{\"invalid\":\"object\"}}"
      val result = scriptTestService.executeEndpointScript("github/pr_merge_hook.py", null, badJson, "john")

      result.exception shouldBe None
      result.stderr should include (badJson)
      result.stderr.toLowerCase should include ("string")

      result.statusCode shouldBe Some(400)
    }

  }

}
