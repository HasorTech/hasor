/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.core;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/** Resolves a custom annotated field before Bean initialization. The container assigns the returned value. */
@FunctionalInterface
public interface AnnotationInjector<A extends Annotation> {
    /**
     * Called once per annotated field each time the container injects an instance, before initialization.
     * The container assigns and type-checks the result; implementations must not start or close the context.
     * A null result is allowed for reference fields. Failures abort injection and initialization.
     */
    Object resolve(AppContext context, Field field, A annotation) throws Throwable;
}
