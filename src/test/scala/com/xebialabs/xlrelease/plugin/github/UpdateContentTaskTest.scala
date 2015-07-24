/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github

import java.nio.charset.StandardCharsets

import com.xebialabs.deployit.plugin.api.reflect.Type
import com.xebialabs.deployit.repository.RepositoryService
import com.xebialabs.platform.script.jython.JythonException
import com.xebialabs.xlrelease.XLReleaseIntegrationScalaTest
import com.xebialabs.xlrelease.builder.PhaseBuilder._
import com.xebialabs.xlrelease.builder.ReleaseBuilder._
import com.xebialabs.xlrelease.builder.TaskBuilder._
import com.xebialabs.xlrelease.domain.{CustomScriptTask, Configuration}
import com.xebialabs.xlrelease.domain.status.TaskStatus._
import com.xebialabs.xlrelease.script.ScriptTestService
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.jgit.util.Base64
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class UpdateContentTaskTest extends XLReleaseIntegrationScalaTest {

  private var gitRepositoryCi: Configuration = _

  val testRepoUrl: String = getSystemPropertyOrFail("github.repository")
  val testRepoUsername: String = getSystemPropertyOrFail("github.username")
  val testRepoPassword: String = getSystemPropertyOrFail("github.password")

  val githubClient = new GitHubClient().setCredentials(testRepoUsername, testRepoPassword)
  val repositoryId = RepositoryId.createFromUrl(testRepoUrl.replace(".git", ""))
  val testFilePath = "test/test.md"

  @Autowired
  private var scriptTestService: ScriptTestService = _

  @Autowired
  private var repositoryService: RepositoryService = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    val configuration: Configuration = Type.valueOf("git.Repository").getDescriptor.newInstance("Configuration/GitRepo")
    configuration.setProperty("title", "GitTestRepo")
    configuration.setProperty("url", testRepoUrl)
    configuration.setProperty("username", testRepoUsername)
    configuration.setProperty("password", testRepoPassword)
    repositoryService.create(configuration)
    gitRepositoryCi = repositoryService.read(configuration.getId)
  }


  describe("github.UpdateContent task") {

    it("should set content of a file in GitHub") {

      val timestamp = System.currentTimeMillis()
      val commitMessage = s"Update test.md with new timestamp: $timestamp"
      val parameters: Map[String, AnyRef] = Map(
        "gitRepository" -> gitRepositoryCi,
        "branch" -> "master",
        "filePath" -> testFilePath,
        "commitMessage" -> commitMessage,
        "regex" -> "^.*$",
        "replacement" -> s"$timestamp"
      )

      val task = executeUpdateContentTask(parameters)

      val commitId: String = task.getPythonScript.getProperty("commitId")
      commitId should not be null

      getGitHubContent(testFilePath) shouldBe s"$timestamp"
      getCommitMessage(commitId) shouldBe commitMessage
    }

    it("should set content of a file in non-default branch") {

      val timestamp = System.currentTimeMillis()
      val parameters: Map[String, AnyRef] = Map(
        "gitRepository" -> gitRepositoryCi,
        "branch" -> "branch-1",
        "filePath" -> testFilePath,
        "commitMessage" -> s"$timestamp",
        "regex" -> "^.*$",
        "replacement" -> s"$timestamp"
      )

      executeUpdateContentTask(parameters)

      getGitHubContent(testFilePath, "branch-1") shouldBe s"$timestamp"
    }

    it("should replace multi-line patterns") {

      setGitHubContent(testFilePath, "111\n\n222\n\n333")
      val parameters: Map[String, AnyRef] = Map(
        "gitRepository" -> gitRepositoryCi,
        "branch" -> "master",
        "filePath" -> testFilePath,
        "commitMessage" -> "test",
        "regex" -> "22\n\n33",
        "replacement" -> "4455"
      )

      executeUpdateContentTask(parameters)

      getGitHubContent(testFilePath) shouldBe "111\n\n244553"
    }

    it("should fail if file does not exist by given path") {

      val parameters: Map[String, AnyRef] = Map(
        "gitRepository" -> gitRepositoryCi,
        "branch" -> "master",
        "filePath" -> "test/bla.md",
        "commitMessage" -> "fail",
        "regex" -> "^.*$",
        "replacement" -> "fail"
      )

      intercept[JythonException] {
        executeUpdateContentTask(parameters)
      }
    }

    it("should not touch file if pattern does not match") {

      val content = getGitHubContent(testFilePath)
      val neverMatchingRegex = "will never match"
      val parameters: Map[String, AnyRef] = Map(
        "gitRepository" -> gitRepositoryCi,
        "branch" -> "master",
        "filePath" -> testFilePath,
        "commitMessage" -> "must not happen",
        "regex" -> neverMatchingRegex,
        "replacement" -> "must not happen"
      )

      val (task, output) = executeUpdateContentTaskWithOutput(parameters)

      val commitId: String = task.getPythonScript.getProperty("commitId")
      commitId shouldBe null

      output should (include("not find") and include(neverMatchingRegex))

      getGitHubContent(testFilePath) shouldBe content
    }

  }

  private def executeUpdateContentTask(parameters: Map[String, AnyRef]): CustomScriptTask = {
    executeUpdateContentTaskWithOutput(parameters)._1
  }

  private def executeUpdateContentTaskWithOutput(parameters: Map[String, AnyRef]): (CustomScriptTask, String) = {
    val task = newCustomScript("github.UpdateContent")
      .withIdAndTitle("DummyTask")
      .withStatus(IN_PROGRESS)
      .withInputParameters(parameters.asJava)
      .withExecutionId.build
    newRelease.withIdAndTitle("DummyRelease")
      .withPhases(
        newPhase.withTasks(task).build)
      .build

    val output = scriptTestService.executeCustomScriptTask(task)
    println(output)
    (task, output)
  }

  private def getSystemPropertyOrFail(name: String) = {
    Option(System.getProperty(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException(
        s"Please specify required test configuration property '$name' using -D$name=<value>")
    }
  }

  private def getGitHubContent(path: String, branch: String = "master"): String = {
    val contentsService = new ContentsService(githubClient)
    val contents = contentsService.getContents(repositoryId, path, branch).get(0)
    new String(Base64.decode(contents.getContent))
  }

  private def setGitHubContent(path: String, content: String, branch: String = "master") = {
    val contentsService = new ContentsService(githubClient)
    val contents = contentsService.getContents(repositoryId, path, branch).get(0)
    contents.setContent(Base64.encodeBytes(content.getBytes(StandardCharsets.UTF_8)))
    contentsService.updateContents(repositoryId, contents, "prepare test data")
  }

  private def getCommitMessage(sha: String) = {
    val commitService = new CommitService(githubClient)
    val commit = commitService.getCommit(repositoryId, sha)
    commit.getCommit.getMessage
  }

}
