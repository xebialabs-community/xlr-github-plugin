package com.xebialabs.gradle.plugins.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task which adds a 'plugin-version.properties' file into the plugin jar file.
 * This file is used by XebiaLabs products to get information about the plugin.
 */
class PluginVersionTask extends DefaultTask {

  @Input
  def plugin

  @Input
  def version

  @OutputFile
  File pluginVersionFile

  @TaskAction
  @SuppressWarnings("GroovyUnusedDeclaration")
  public void addProperties() {
    pluginVersionFile.createNewFile()
    pluginVersionFile.withWriter { w ->
      w.writeLine("plugin=${getPlugin()}")
      w.writeLine("version=${getVersion()}")
    }
  }
}
