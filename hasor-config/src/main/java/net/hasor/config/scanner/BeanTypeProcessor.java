/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.config.scanner;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.config.Bean;
import net.hasor.core.ApiBinder;
import net.hasor.core.ApiBinder.LifeBindingBuilder;
import net.hasor.core.ApiBinder.NamedBindingBuilder;
import net.hasor.core.DependsOn;

/** Registers scanned @Bean classes through Hasor's normal injection and lifecycle bindings. */
public final class BeanTypeProcessor implements AnnotationProcessor<Class<?>> {
    @Override
    public List<Class<? extends Annotation>> annotationTypes() {
        return List.of(Bean.class);
    }

    @Override
    public void process(ApiBinder binder, List<Class<?>> types) {
        for (Class<?> type : types) {
            this.register(binder, type);
        }
    }

    private <T> void register(ApiBinder binder, Class<T> type) {
        Bean bean = type.getAnnotation(Bean.class);
        if (bean == null) {
            return;
        }
        if (type.isInterface() || type.isEnum() || Modifier.isAbstract(type.getModifiers()) || (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))) {
            throw new IllegalArgumentException("@Bean must annotate a concrete, independently constructible class: " + type.getName());
        }

        // Explicit bindings and previously scanned registrations retain ownership of this type.
        if (!binder.findBindingRegister(type).isEmpty()) {
            return;
        }

        String name = StringUtils.isBlank(bean.value()) ? StringUtils.uncapitalize(type.getSimpleName()) : bean.value();
        if (binder.getBindInfo(name) != null) {
            throw new IllegalStateException("Conflicting @Bean name '" + name + "' for " + type.getName());
        }
        NamedBindingBuilder<T> named = binder.bindType(type);
        LifeBindingBuilder<T> binding = StringUtils.isBlank(bean.value()) ? named.idWith(name) : named.bothWith(name);
        DependsOn dependencies = type.getAnnotation(DependsOn.class);
        if (dependencies != null) {
            binding.dependsOn(dependencies.value());
            binding.dependsOn(dependencies.types());
        }
        if (StringUtils.isNotBlank(bean.initMethod())) {
            binding.initMethod(bean.initMethod());
        }
        if (StringUtils.isNotBlank(bean.destroyMethod())) {
            binding.destroyMethod(bean.destroyMethod());
        }
        if (bean.singleton()) {
            binding.asEagerSingleton();
        } else {
            binding.asEagerPrototype();
        }
    }
}
