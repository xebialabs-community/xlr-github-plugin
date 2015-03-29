package com.xebialabs.gradle.plugins

import org.gradle.api.Project

/**
 * Extension to setup configuration of a plugin of one of XebiaLabs products.
 */
class XlPluginExtension {
  Project project

  /**
   * Defines which extension the plugin file will have.
   * Usually it is ".xldp" for XL Deploy and ".xlrp" for XL Release.
   */
  def pluginExtension

  /**
   * An array of patterns used to determine which test classes are unit tests and will be run
   * by 'test' task, and which are integration tests and will be run by 'itest' task.
   */
  def itestClassPatterns

}
