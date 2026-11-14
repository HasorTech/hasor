/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.boot;
import java.net.URLClassLoader;
import java.util.Objects;
import net.hasor.boot.scan.beans.BeanSource;
import net.hasor.boot.scan.first.FirstSource;
import net.hasor.boot.scan.second.SecondSource;
import net.hasor.cobble.StringUtils;
import net.hasor.core.AppContext;
import net.hasor.core.Hasor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
public class BootSourceScanTest {
    @Test
    public void sourcePackageRegistersBeanClassesAndInjectsDependencies() throws Exception {
        Boot boot = new Boot().sources(BeanSource.class);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            BeanSource.Service service = context.getInstance(BeanSource.Service.class);
            assertEquals("scanned", service.message());
            assertSame(service, context.getInstance("service"));
        }
    }

    @Test
    public void sourcePackageIncludesChildConfigurations() throws Exception {
        Boot boot = new Boot().sources(FirstSource.class);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("first", context.findBindingBean("firstMessage", String.class));
            assertNull(context.findBindingBean("secondMessage", String.class));
            assertNull(context.findBindingBean("explicitMessage", String.class));
            assertNull(context.findBindingBean("neighborMessage", String.class));
        }
    }

    @Test
    public void sourcesUseTheUnionOfTheirPackages() throws Exception {
        Boot boot = new Boot().sources(FirstSource.class, SecondSource.class);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("first", context.findBindingBean("firstMessage", String.class));
            assertEquals("second", context.findBindingBean("secondMessage", String.class));
            assertEquals(FirstSource.class.getPackageName() + "," + SecondSource.class.getPackageName(), context.getSettings().getString("hasor.loadPackages"));
        }
    }

    @Test
    public void sourcesInTheSamePackageShareOneScanRange() throws Exception {
        Boot boot = new Boot().sources(FirstSource.class, FirstSource.AdditionalSource.class);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals(FirstSource.class.getPackageName(), context.getSettings().getString("hasor.loadPackages"));
            assertEquals(1, context.findBindingBean(String.class).stream().filter(value -> StringUtils.equals(value, "first")).count());
        }
    }

    @Test
    public void explicitPropertyReplacesSourcePackageScanning() throws Exception {
        Boot boot = new Boot().sources(FirstSource.class)//
                .property("hasor.loadPackages", "net.hasor.boot.scan.explicit");
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("explicit", context.findBindingBean("explicitMessage", String.class));
            assertNull(context.findBindingBean("firstMessage", String.class));
            assertNotNull(context.getInstance(FirstSource.class));
        }
    }

    @Test
    public void explicitEmptyPropertyDisablesPackageScanning() throws Exception {
        Boot boot = new Boot().sources(FirstSource.class)//
                .property("hasor.loadPackages", "");
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("", context.getSettings().getString("hasor.loadPackages"));
            assertNull(context.findBindingBean("firstMessage", String.class));
            assertNotNull(context.getInstance(FirstSource.class));
        }
    }

    @Test
    public void environmentScanRangeReplacesSourcePackageScanning() throws Exception {
        String original = System.getProperty("HASOR_LOAD_PACKAGES");
        System.setProperty("HASOR_LOAD_PACKAGES", "net.hasor.boot.scan.explicit");
        Boot boot = new Boot().sources(FirstSource.class);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("net.hasor.boot.scan.explicit", context.getSettings().getString("hasor.loadPackages"));
            assertEquals("explicit", context.findBindingBean("explicitMessage", String.class));
            assertNull(context.findBindingBean("firstMessage", String.class));
        } finally {
            if (original == null) {
                System.clearProperty("HASOR_LOAD_PACKAGES");
            } else {
                System.setProperty("HASOR_LOAD_PACKAGES", original);
            }
        }
    }

    @Test
    public void explicitPropertyOverridesConfigurationFileScanRange() throws Exception {
        Boot boot = new Boot().sources(FirstSource.class)//
                .hconfigFile("boot-source-scan/explicit.properties")//
                .property("hasor.loadPackages", SecondSource.class.getPackageName());
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("second", context.findBindingBean("secondMessage", String.class));
            assertNull(context.findBindingBean("firstMessage", String.class));
            assertNull(context.findBindingBean("explicitMessage", String.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "properties", "xml" })
    public void applicationSchemaScanRangeReplacesSourcePackageScanning(String extension) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        var resources = Objects.requireNonNull(original.getResource("boot-source-scan/schema-" + extension + "/"));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] { resources }, original)) {
            Thread.currentThread().setContextClassLoader(loader);
            Boot boot = new Boot().sources(FirstSource.class);
            try (BootApplication application = boot.start()) {
                AppContext context = application.getAppContext();
                assertEquals("net.hasor.boot.scan.explicit", context.getSettings().getString("hasor.loadPackages"));
                assertEquals("explicit", context.findBindingBean("explicitMessage", String.class));
                assertNull(context.findBindingBean("firstMessage", String.class));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "properties", "xml", "yaml" })
    public void configurationFileReplacesSourcePackageScanning(String extension) throws Exception {
        Boot boot = new Boot().sources(FirstSource.class)//
                .hconfigFile("boot-source-scan/explicit." + extension);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("net.hasor.boot.scan.explicit", context.getSettings().getString("hasor.loadPackages"));
            assertEquals("explicit", context.findBindingBean("explicitMessage", String.class));
            assertNull(context.findBindingBean("firstMessage", String.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "properties", "xml", "yaml" })
    public void explicitEmptyConfigurationFileDisablesPackageScanning(String extension) throws Exception {
        Boot boot = new Boot().sources(FirstSource.class)//
                .hconfigFile("boot-source-scan/empty." + extension);
        try (BootApplication application = boot.start()) {
            AppContext context = application.getAppContext();
            assertEquals("", context.getSettings().getString("hasor.loadPackages"));
            assertNull(context.findBindingBean("firstMessage", String.class));
            assertNotNull(context.getInstance(FirstSource.class));
        }
    }

    @Test
    public void noSourcesPreserveHasorDefaultScanRange() throws Exception {
        Boot boot = new Boot();
        try (BootApplication application = boot.start()) {
            assertEquals(Hasor.create().buildSettings().getString("hasor.loadPackages"), application.getAppContext().getSettings().getString("hasor.loadPackages"));
        }
    }
}
