package com.xebialabs.gradle.plugins.tasks

import com.xebialabs.gradle.plugins.XlReleasePluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import static com.xebialabs.gradle.plugins.tasks.StopXlReleaseTask.waitForUrl

/**
 * Gradle task which starts an XL Release instance with current plugin included.
 */
class StartXlReleaseTask extends DefaultTask {

  @Input
  def xlReleaseHome

  @TaskAction
  @SuppressWarnings("GroovyUnusedDeclaration")
  public void startXlRelease() {

    StopXlReleaseTask.stopXlRelease(logger)

    logger.lifecycle("Starting XL Release server ...")

    def extension = project.extensions.findByType(XlReleasePluginExtension)

    File license = project.file(extension.xlReleaseLicense)
    if (!license.exists()) {
      throw new RuntimeException("Could not find license, please specify location of XL Release license file in your " +
          "gradle.properties or with '-PxlReleaseLicense=/path/to/xl-release-license.lic' command line option")
    }

    project.copy {
      from license
      into "${project.buildDir}/server/conf/"
    }

    def configDefaultsFile = project.file("${project.buildDir}/server/conf/xl-release-server.conf.defaults")
    configDefaultsFile.createNewFile()
    configDefaultsFile.write(CONFIG_DEFAULTS)

    def policyFile = project.file(extension.xlReleasePolicy)
    logger.info("Using following xl-release.policy file: $policyFile")

    def classpath = [
        "${project.buildDir}/server/conf",
        "$xlReleaseHome/conf",
        "$xlReleaseHome/ext",
        "$xlReleaseHome/hotfix/*",
        "$xlReleaseHome/lib/*",
        "$xlReleaseHome/plugins/*"
    ]
    classpath += [
        "${project.buildDir}/classes/test",
        "${project.buildDir}/classes/main",
        "${project.buildDir}/resources/test",
        "${project.buildDir}/resources/main"
    ].findAll { project.file(it).exists() }

    logger.debug("Using following classpath to start XL Release: $classpath")

    ant.java(
        fork: true,
        spawn: true,
        classpath: classpath.join(':'),
        dir: "${project.buildDir}/server/",
        classname: "com.xebialabs.xlrelease.XLReleaseBootstrapper") {
      jvmarg(value: "-Xmx1024m")
      jvmarg(value: "-XX:MaxPermSize=256m")
      jvmarg(value: "-Djava.awt.headless=true")
      jvmarg(value: "-Dlogback.configurationFile=$xlReleaseHome/conf/logback.xml")
      jvmarg(value: "-Dderby.stream.error.file=log/derby.log")
      jvmarg(value: "-Djava.security.manager=java.lang.SecurityManager")
      jvmarg(value: "-Djava.security.policy=$policyFile")
      jvmarg(value: "-Djava.net.preferIPv4Stack=true")
      jvmarg(value: "-XX:-OmitStackTraceInFastThrow")
      jvmarg(value: "-Xverify:none")

      arg(value: "-reinitialize")
      arg(value: "-force")
      arg(value: "-force-upgrades")
      arg(value: "-setup-defaults")
      arg(value: "${configDefaultsFile}")

      if (project.hasProperty("debug")) {
        logger.lifecycle("Enabled debug mode on port 5005")
        jvmarg(value: "-Xdebug")
        jvmarg(value: "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005")
      }
    }

    def url = 'http://localhost:5516'
    waitForUrl(url, true, logger)

    logger.lifecycle("XL Release has started at $url")
  }

  private static final String CONFIG_DEFAULTS = """\
    # XL Release configuration file.
    admin.password={b64}YFKOzMTEICsqFJ592l2FbQ\\=\\=
    jcr.repository.path=repository
    threads.min=3
    ssl=false
    client.session.remember.enabled=true
    http.bind.address=0.0.0.0
    http.context.root=/
    threads.max=24
    client.session.timeout.minutes=0
    http.port=5516
    hide.internals=false
    importable.packages.path=importablePackages""".stripIndent()

}
