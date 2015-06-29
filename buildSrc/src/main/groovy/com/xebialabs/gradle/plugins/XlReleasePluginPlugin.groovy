package com.xebialabs.gradle.plugins

import com.xebialabs.gradle.plugins.tasks.StartXlReleaseTask
import com.xebialabs.gradle.plugins.tasks.StopXlReleaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin applied to projects which build plugins for XL Release.
 */
class XlReleasePluginPlugin implements Plugin<Project> {

  public static final String START_TASK_NAME = 'start'
  public static final String STOP_TASK_NAME = 'stop'
  public static final String XL_RELEASE_JARS_CONFIGURATION = 'xlReleaseJars'
  public static final String ADDITIONAL_CLASSPATH_CONFIGURATION = 'xlReleaseAdditionalClasspath'
  public static final String XL_RELEASE_PLUGIN_EXTENSION = "xlReleasePlugin"

  @Override
  void apply(final Project project) {
    project.plugins.apply(XlPluginPlugin)
    project.extensions.findByType(XlPluginExtension).pluginExtension = "xlrp"

    configureExtensions(project)
    configureConfigurations(project)

    def start = configureStartTask(project)
    def stop = configureStopTask(project)

    project.tasks.getByName('itest').dependsOn start
    project.tasks.getByName('itest').finalizedBy stop
  }

  private static def configureExtensions(Project p) {
    p.extensions.create(XL_RELEASE_PLUGIN_EXTENSION, XlReleasePluginExtension).with {
      project = p

      xlReleaseLicense = { ->
        if (project.hasProperty('xlReleaseLicense')) {
          project.property('xlReleaseLicense')
        } else {
          "$xlReleaseHome/conf/xl-release-license.lic"
        }
      }

      xlReleasePolicy = { ->
        if (project.file("${project.buildDir}/src/main/resources/xl-release.policy").exists()) {
          "${project.buildDir}/src/main/resources/xl-release.policy"
        } else {
          "$xlReleaseHome/conf/xl-release.policy"
        }
      }

      xlReleaseHome = project.hasProperty('xlReleaseHome') ? project.property('xlReleaseHome') : System.env.XLRELEASE_HOME
    }

    p.afterEvaluate {
      def xlrHome = p.extensions.findByType(XlReleasePluginExtension).xlReleaseHome
      if (!xlrHome) {
        throw new RuntimeException("Please specify location of unpacked XL Release server installation in your " +
            "gradle.properties or with '-PxlReleaseHome=/path/to/xl/release/server' command line option")
      }
      def folder = p.file(xlrHome)
      if (!folder.exists() || !folder.isDirectory()) {
        throw new RuntimeException("$xlrHome does not exist or is not a folder")
      }
      p.logger.lifecycle("Using XL Release libraries from $folder")
    }
  }

  private static def configureConfigurations(Project project) {
    project.configurations.create(XL_RELEASE_JARS_CONFIGURATION)
    project.dependencies.add(XL_RELEASE_JARS_CONFIGURATION, project.fileTree(
        dir: "${project.extensions.findByType(XlReleasePluginExtension).xlReleaseHome}/lib")
    )
    project.configurations.create(ADDITIONAL_CLASSPATH_CONFIGURATION)
  }

  def static configureStartTask(final Project project) {
    project.tasks.create(START_TASK_NAME, StartXlReleaseTask).configure {
      group = "other"
      description = "Starts XL Release server with current plugin included."
      dependsOn(['classes'])
      xlReleaseHome = { -> project.extensions.findByType(XlReleasePluginExtension).xlReleaseHome }
      useSourcesDirectly = { -> project.extensions.findByType(XlReleasePluginExtension).useSourcesDirectly }
    }
  }

  def static configureStopTask(final Project project) {
    project.tasks.create(STOP_TASK_NAME, StopXlReleaseTask).configure {
      group = "other"
      description = "Stops XL Release server if it is running."
    }
  }
}
