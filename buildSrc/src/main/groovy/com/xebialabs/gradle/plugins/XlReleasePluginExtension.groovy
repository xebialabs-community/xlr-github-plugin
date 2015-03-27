package com.xebialabs.gradle.plugins

import org.gradle.api.Project

/**
 * Extension which allows to configure Gradle XL Release Plugin.
 */
class XlReleasePluginExtension {
  Project project

  /**
   * Location of unpacked XL Release server installation. May be a string or a file.
   */
  def xlReleaseHome

  /**
   * Location of XL Release license to use for testing. May be string or file.
   */
  def xlReleaseLicense

  /**
   * Location of custom xl-release.policy file if you want to override the default one.
   */
  def xlReleasePolicy
}
