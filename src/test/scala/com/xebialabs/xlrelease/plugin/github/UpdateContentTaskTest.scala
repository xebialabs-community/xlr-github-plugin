package com.xebialabs.xlrelease.plugin.github

import com.xebialabs.deployit.plugin.api.reflect.Type
import com.xebialabs.xlrelease.builder.PhaseBuilder._
import com.xebialabs.xlrelease.builder.ReleaseBuilder._
import com.xebialabs.xlrelease.builder.TaskBuilder._
import com.xebialabs.xlrelease.domain.Configuration
import com.xebialabs.xlrelease.domain.status.TaskStatus._
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.jgit.util.Base64
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class UpdateContentTaskTest extends BaseXLReleaseTestServerTest {

  private var gitRepositoryCi: Configuration = _

  val testRepoUrl: String = getSystemPropertyOrFail("github.repository")
  val testRepoUsername: String = getSystemPropertyOrFail("github.username")
  val testRepoPassword: String = getSystemPropertyOrFail("github.password")

  val githubClient = new GitHubClient().setCredentials(testRepoUsername, testRepoPassword)
  val repositoryId = RepositoryId.createFromUrl(testRepoUrl.replace(".git", ""))

  override def beforeAll(): Unit = {
    super.beforeAll()
    val configuration: Configuration = Type.valueOf("git.Repository").getDescriptor.newInstance("Configuration/Custom/GitRepo")
    configuration.setProperty("title", "GitTestRepo")
    configuration.setProperty("url", testRepoUrl)
    configuration.setProperty("username", testRepoUsername)
    configuration.setProperty("password", testRepoPassword)
    server.getRepositoryService.create(configuration)
    gitRepositoryCi = server.getRepositoryService.read(configuration.getId)
  }


  describe("github.UpdateContent custom script task") {

    it("should set content of a file in GitHub") {

      val timestamp = System.currentTimeMillis()
      val filePath = "test/test.md"
      val commitMessage = s"Update test.md with new timestamp: $timestamp"
      val parameters: Map[String, AnyRef] = Map(
        "gitRepository" -> gitRepositoryCi,
        "branch" -> "master",
        "filePath" -> filePath,
        "commitMessage" -> commitMessage,
        "regex" -> "^.*$",
        "replacement" -> s"$timestamp"
      )

      val task = newCustomScript("github.UpdateContent")
        .withIdAndTitle("DummyTask")
        .withStatus(IN_PROGRESS)
        .withInputParameters(parameters.asJava)
        .withExecutionId.build
      newRelease.withIdAndTitle("DummyRelease")
        .withPhases(
          newPhase.withTasks(task).build)
        .build

      val output = server.getScriptTestService.executeCustomScriptTask(task)
      println(output)

      val commitId: String = task.getPythonScript.getProperty("commitId")
      commitId should not be null

      getContentFromGitHub(filePath) shouldBe s"$timestamp"
      getCommitMessage(commitId) shouldBe commitMessage
    }

  }

  private def getSystemPropertyOrFail(name: String) = {
    Option(System.getProperty(name)) match {
      case Some(s) => s
      case None => throw new RuntimeException(
        s"Please specify required test configuration property '$name' using -D$name=<value>")
    }
  }

  private def getContentFromGitHub(path: String): String = {
    val contentsService = new ContentsService(githubClient)
    val contents = contentsService.getContents(repositoryId, path).get(0)
    new String(Base64.decode(contents.getContent))
  }

  private def getCommitMessage(sha: String) = {
    val commitService = new CommitService(githubClient)
    val commit = commitService.getCommit(repositoryId, sha)
    commit.getCommit.getMessage
  }

}
