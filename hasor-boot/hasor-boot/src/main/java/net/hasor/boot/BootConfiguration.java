/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.boot;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.setting.DefaultSettings;
import net.hasor.cobble.setting.SettingNode;
import net.hasor.cobble.setting.Settings;
import net.hasor.config.Configuration;
import net.hasor.config.ConfigurationModule;
import net.hasor.core.Hasor;

/** 单次启动的配置快照，不区分应用类型。 */
public final class BootConfiguration {
    private final String[]            arguments;
    private final Class<?>[]          sources;
    private final String              hconfigFile;
    private final ClassLoader         classLoader;
    private final Map<String, Object> properties;
    private final Settings            settings;

    BootConfiguration(String[] arguments, Class<?>[] sources, String hconfigFile, ClassLoader classLoader, Map<String, Object> properties) throws IOException {
        this.arguments = arguments.clone();
        this.sources = sources.clone();
        this.hconfigFile = hconfigFile;
        this.classLoader = classLoader;
        this.properties = new LinkedHashMap<>(properties);
        this.settings = newHasor(null).buildSettings();
        configureScanPackages();
    }

    private void configureScanPackages() throws IOException {
        if (this.sources.length == 0 || this.properties.containsKey("hasor.loadPackages")) {
            return;
        }

        // 主配置与框架默认配置分开检查，避免将 Core 的内置范围误认为用户配置。
        Settings configured = new DefaultSettings(this.hconfigFile);
        if (configured.getNode("hasor.loadPackages") != null || StringUtils.isNotBlank(configured.getEnv("HASOR_LOAD_PACKAGES"))) {
            return;
        }

        for (String namespace : this.settings.getSettingArray()) {
            SettingNode scope = this.settings.getSettings(namespace).getNode("hasor.loadPackages");
            if (scope == null) {
                continue;
            }
            if (StringUtils.equals(namespace, Settings.DefaultNameSpace)) {
                return;
            }
            // 兼容旧 main 命名空间；Core 仅声明一项，应用 schemas 追加的范围同样优先。
            if (StringUtils.equals(namespace, "http://www.hasor.net/sechma/main") && this.settings.getSettings(namespace).getStringArray("hasor.loadPackages").length > 1) {
                return;
            }
        }

        Set<String> packages = new LinkedHashSet<>();
        for (Class<?> source : this.sources) {
            String name = source.getPackageName();
            if (StringUtils.isNotBlank(name)) {
                packages.add(name);
            }
        }

        if (!packages.isEmpty()) {
            String scope = String.join(",", packages);
            this.properties.put("hasor.loadPackages", scope);
            this.settings.setSetting("hasor.loadPackages", scope);
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public String[] getArguments() {
        return arguments.clone();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    Hasor newHasor(Object environment) {
        Hasor hasor = Hasor.create(environment).classLoaderWith(classLoader).mainSettingWith(hconfigFile).bindArguments(arguments).registerShutdownHook(false);
        properties.forEach((key, value) -> hasor.addSettings(Settings.DefaultNameSpace, key, value));
        return hasor;
    }

    void configureSources(Hasor hasor) {
        for (Class<?> source : sources) {
            if (source.isAnnotationPresent(Configuration.class)) {
                hasor.addModules(ConfigurationModule.of(source));
            } else {
                hasor.addPrimarySources(source);
            }
        }
    }
}