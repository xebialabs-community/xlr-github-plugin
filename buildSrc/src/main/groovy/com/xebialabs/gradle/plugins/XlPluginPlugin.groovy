package com.xebialabs.gradle.plugins

import com.xebialabs.gradle.plugins.tasks.PluginVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.Jar

/**
 * Gradle plugin which is applied to any project which builds a plugin for one of XebiaLabs products.
 */
class XlPluginPlugin implements Plugin<Project> {

  public static final String PLUGIN_VERSION_TASK_NAME = "createPluginVersionProperties"

  @Override
  void apply(final Project project) {
    project.plugins.apply("java")

    configureExtensions(project)
    setupDependencyBundling(project)

    configurePluginVersionTask(project)
    configurePluginPackagingTask(project)
  }

  private static void configurePluginPackagingTask(Project project) {
    def xlpTask = project.tasks.create("xlp", Jar).configure {
      group = BasePlugin.BUILD_GROUP
      description = "Packages plugin together with required libraries in one file to be used in one of XebiaLabs products"
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE

      into(".") {
        from project.configurations.getByName("distBundle")
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

  private static String configureExtensions(Project p) {
    p.extensions.create("xl-plugin", XlPluginExtension).with {
      project = p
      pluginExtension = "xlp"
    }
  }

  private static def setupDependencyBundling(Project project) {
    project.configurations.maybeCreate("distBundle")
  }
}
