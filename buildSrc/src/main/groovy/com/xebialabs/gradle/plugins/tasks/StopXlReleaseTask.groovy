package com.xebialabs.gradle.plugins.tasks

import groovyx.net.http.RESTClient
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task which stop a running XL Release instance.
 */
class StopXlReleaseTask extends DefaultTask {

  @TaskAction
  @SuppressWarnings("GroovyUnusedDeclaration")
  public void stopXlRelease() {
    stopXlRelease(logger)
  }

  static def stopXlRelease(Logger logger) {
    def url = 'http://localhost:5516'
    logger.lifecycle("Stopping XL Release server at ${url}")
    def http = new RESTClient(url)
    try {
      http.post(path: "/server/shutdown", headers: ['Authorization': 'Basic YWRtaW46YWRtaW4='])
    } catch (ex) {
      logger.info(ex.message)
    }
    waitForUrl(url, false, logger)
  }

  static def waitForUrl(String url, boolean toBeAvailable, Logger logger) {
    def http = new RESTClient(url)
    http.handler.'success' = {}
    http.handler.'failure' = {}

    def seconds = 30
    while (seconds > 0) {
      try {
        http.get([:])
        if (toBeAvailable) return
      } catch (ConnectException ignored) {
        if (!toBeAvailable) return
      }
      logger.info("Waiting $seconds seconds for $url to become ${toBeAvailable ? "available" : "unavailable"}")
      Thread.sleep(2000)
      seconds -= 2
    }

    throw new RuntimeException("$url did not become ${toBeAvailable ? "available" : "unavailable"} in timely manner. " +
        "You can check log file 'build/server/log/xl-release.log' for more details.")
  }

}
