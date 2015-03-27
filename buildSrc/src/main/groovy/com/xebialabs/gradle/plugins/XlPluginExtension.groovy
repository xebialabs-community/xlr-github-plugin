package com.xebialabs.gradle.plugins

import org.gradle.api.Project

/**
 * Extension which allows setting which extension the plugin file will have.
 * Usually it is ".xldp" for XL Deploy and ".xlrp" for XL Release.
 */
class XlPluginExtension {
  Project project

  def pluginExtension
}
