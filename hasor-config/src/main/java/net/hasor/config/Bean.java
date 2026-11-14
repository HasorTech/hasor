/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.config;
import java.lang.annotation.*;

/** Registers a scanned class or the result of a factory method as a Hasor-managed bean. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Bean {
    /** Bean name. Defaults to the factory method name or the class name with a lowercase initial. */
    String value() default "";

    /** Whether the bean is a singleton. */
    boolean singleton() default true;

    /** Optional initialization method on the returned bean. */
    String initMethod() default "";

    /** Optional destruction method on the returned bean. */
    String destroyMethod() default "";
}
