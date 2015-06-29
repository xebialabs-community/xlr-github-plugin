/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.xlrelease.plugin.github;

import java.io.File;
import org.junit.BeforeClass;
import com.google.common.io.Files;

import com.xebialabs.xlrelease.test.XLReleaseTestBootstrapper;
import com.xebialabs.xlrelease.test.XLReleaseTestServer;

/**
 * Base class for JUnit tests that make use of [[XLReleaseTestServer]].
 */
public abstract class BaseXLReleaseTestServerTest {

    protected static File tempDir;
    protected static XLReleaseTestServer server;

    /**
     * Initializes an XL Release server in a temporary folder.
     *
     * <strong>Note:</strong> XL Release is initialized once per JVM, so a repository
     * created for one test will be reused in other tests.
     */
    @BeforeClass
    public static void beforeAll() {
        if (server == null) {
            tempDir = Files.createTempDir();
            String licenseLocation = System.getProperty("xlReleaseLicense");
            if (licenseLocation == null) {
                throw new RuntimeException("Please specify XL Release license location using " +
                        "-DxlReleaseLicense=/path/to/xl-release-license.lic");
            }
            XLReleaseTestBootstrapper bootstrapper = new XLReleaseTestBootstrapper(tempDir,
                    new File(licenseLocation));
            server = bootstrapper.start();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.stop();
                    org.assertj.core.util.Files.delete(tempDir);
                }
            }));
        }
    }

}
