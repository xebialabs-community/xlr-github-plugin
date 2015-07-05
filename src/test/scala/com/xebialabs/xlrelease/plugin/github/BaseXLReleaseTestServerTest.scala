package com.xebialabs.xlrelease.plugin.github

import java.io.File

import com.google.common.io.Files
import com.xebialabs.xlrelease.test.{XLReleaseTestBootstrapper, XLReleaseTestServer}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

import scalax.file.Path

trait BaseXLReleaseTestServerTest extends FunSpecLike with Matchers with BeforeAndAfterAll  {

  protected var tempDir: File = _
  protected var server: XLReleaseTestServer = _

  /**
   * Initializes an XL Release server in a temporary folder.
   *
   * <strong>Note:</strong> XL Release is initialized once per JVM, so a repository
   * created for one test will be reused in other tests.
   */
  override def beforeAll() = {
    if (server == null) {
      tempDir = Files.createTempDir
      val licenseFile: File = System.getProperty("xlReleaseLicense") match {
        case s: String => new File(s)
        case _ => throw new RuntimeException("Please specify XL Release license location using " + "-DxlReleaseLicense=/path/to/xl-release-license.lic")
      }
      val bootstrapper = new XLReleaseTestBootstrapper(tempDir, licenseFile)
      server = bootstrapper.start
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
        def run() {
          server.stop()
          Path(tempDir).deleteRecursively(force = true, continueOnFailure = true)
        }
      }))
    }
  }
}
