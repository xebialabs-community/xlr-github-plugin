/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github

import com.xebialabs.xlrelease.client.XlrClient
import com.xebialabs.xlrelease.domain._
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpecLike, Matchers}
import spray.http.StatusCodes

@RunWith(classOf[JUnitRunner])
class UpdateContentTaskItest extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(60, Minutes)), interval = scaled(Span(50, Millis)))

  describe("UpdateContentTask") {

    it("should update content of a file in remote repository") {
      val client = new XlrClient("http://localhost:5516")
      val directory = Directory("Configuration/Custom")
      val repo = HttpConnection(s"${directory.id}/ConfigurationGitRepo", "TestGitRepo", "git.Repository")

      val release = Release.build("ReleaseTestUpdateContentTask")
      val phase = Phase.build("Phase", release.id)
      val task = Task.build("TaskUpdateContent", phase.id)
      val cis = Seq(directory, repo, release, phase, task)
      val response = client.createCis(cis).futureValue
      response.status should (be(StatusCodes.OK) or be(StatusCodes.NoContent))
    }
  }
}
