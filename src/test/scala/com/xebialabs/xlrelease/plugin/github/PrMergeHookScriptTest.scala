package com.xebialabs.xlrelease.plugin.github

import com.xebialabs.xlplatform.endpoints.actors.ScriptDone
import com.xebialabs.xlrelease.test.script.ScriptTestService
import org.assertj.core.api.Assertions._
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
