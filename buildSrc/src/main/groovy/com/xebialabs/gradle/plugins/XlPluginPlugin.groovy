package com.xebialabs.gradle.plugins

import com.xebialabs.gradle.plugins.tasks.PluginVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

/**
 * Gradle plugin which is applied to any project which builds a plugin for one of XebiaLabs products.
 */
class XlPluginPlugin implements Plugin<Project> {

  public static final String PLUGIN_VERSION_TASK_NAME = "createPluginVersionProperties"
  public static final String XL_PLUGIN_BUNDLE_CONFIGURATION = "xlPluginBundle"
  public static final String ITEST_TASK_NAME = "itest"
  public static final String XL_PLUGIN_EXTENSION = "xlPlugin"

  @Override
  void apply(final Project project) {
    project.plugins.apply("java")

    configureExtensions(project)
    setupDependencyBundling(project)

    configurePluginVersionTask(project)
    configurePluginPackagingTask(project)
    configureItestTask(project)
  }

  private static void configurePluginPackagingTask(Project project) {
    def xlpTask = project.tasks.create("xlp", Jar).configure {
      group = BasePlugin.BUILD_GROUP
      description = "Packages plugin together with required libraries in one file to be used in one of XebiaLabs products"
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE

      into(".") {
        from project.configurations.getByName(XL_PLUGIN_BUNDLE_CONFIGURATION)
        from project.tasks.getByName("jar").outputs
      }
    } as Jar

    // Let override the file extension in the build script or other plugins
    project.afterEvaluate {
      xlpTask.configure {
        extension = "${project.extensions.getByType(XlPluginExtension).pluginExtension}"
      }
    }

    project.artifacts {
      archives xlpTask
    }
  }

  private static void configurePluginVersionTask(Project project) {
    project.tasks.create(PLUGIN_VERSION_TASK_NAME, PluginVersionTask).configure {
      group = BasePlugin.BUILD_GROUP
      description = "Create a plugin-version.properties file with plugin metadata."
      dependsOn("processResources")
      plugin = { -> project.name }
      version = { -> project.version }
      pluginVersionFile = project.file("${project.sourceSets.main.output.classesDir}/plugin-version.properties")
    }
    project.tasks.getByName("jar").dependsOn(PLUGIN_VERSION_TASK_NAME)
  }

  private static void configureItestTask(Project project) {

    project.tasks.create(ITEST_TASK_NAME, Test).configure {
      group = JavaBasePlugin.VERIFICATION_GROUP
      description = "Run the integration tests."
      reports.junitXml.destination = project.file("${project.buildDir}/itest-results")
    }

    project.afterEvaluate {
      def extension = project.extensions.getByType(XlPluginExtension)

      project.tasks.getByName("test").configure {
        excludes = extension.itestClassPatterns
      }
      project.tasks.getByName(ITEST_TASK_NAME).configure {
        includes = extension.itestClassPatterns
      }
    }
  }

  private static String configureExtensions(Project p) {
    p.extensions.create(XL_PLUGIN_EXTENSION, XlPluginExtension).with {
      project = p
      pluginExtension = "xlp"
      itestClassPatterns = ["**/*Itest.*", "**/*ItestSuite.*"]
    }
  }

  private static def setupDependencyBundling(Project project) {
    project.configurations.maybeCreate(XL_PLUGIN_BUNDLE_CONFIGURATION)
  }
}
