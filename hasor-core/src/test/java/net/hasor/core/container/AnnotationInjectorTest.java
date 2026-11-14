/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.core.container;
import java.lang.annotation.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.core.*;
import net.hasor.core.binder.ApiBinderWrap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationInjectorTest {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Custom {}
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Other {}

    public static class Parent {
        @Custom
        private String value;
        String value() { return this.value; }
        public void setValue(String value) { this.value = value; }
    }
    public static class Client extends Parent {
        boolean initialized;
        @Init
        public void init() { this.initialized = "injected".equals(value()); }
    }
    public static class Primitive {
        @Custom
        int value;
    }
    public static class Conflicting {
        @Custom @Inject
        String value;
    }
    public static class Multiple {
        @Custom @Other
        String value;
    }
    public static class Immutable {
        @Custom
        final String value = "original";
    }
    public static class StaticField {
        @Custom
        static String value;
    }

    @Test
    void injectsInheritedFieldBeforeInitializationThroughBinderWrapper() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            new ApiBinderWrap(binder).bindAnnotationInjector(Custom.class, (context, field, annotation) -> "injected");
        })) {
            Client bean = app.getInstance(Client.class);
            assertEquals("injected", bean.value());
            assertTrue(bean.initialized);
        }
    }

    @Test
    void resolvesEachPrototypeInstance() throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "value-" + count.incrementAndGet());
        })) {
            assertEquals("value-1", app.getInstance(Parent.class).value());
            assertEquals("value-2", app.getInstance(Parent.class).value());
        }
    }

    @Test
    void supportsPrimitiveFields() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> 42);
        })) {
            assertEquals(42, app.getInstance(Primitive.class).value);
        }
    }

    @Test
    void resolvesSingletonOnlyOnce() throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "value-" + count.incrementAndGet());
            binder.bindType(Parent.class).asEagerSingleton();
        })) {
            assertSame(app.getInstance(Parent.class), app.getInstance(Parent.class));
            assertEquals(1, count.get());
        }
    }

    @Test
    void preservesExplicitPropertyOverride() throws Exception {
        AtomicInteger count = new AtomicInteger();
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "value-" + count.incrementAndGet());
            binder.bindType(Parent.class).injectValue("value", "explicit").overwriteAnnotation();
        })) {
            assertEquals("explicit", app.getInstance(Parent.class).value());
            assertEquals(0, count.get());
        }
    }

    @Test
    void rejectsDuplicateRegistration() {
        assertThrows(IllegalStateException.class, () -> Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "first");
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "second");
        }));
    }

    @Test
    void rejectsConflictingNativeInjection() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "value");
        })) {
            assertThrows(IllegalStateException.class, () -> app.getInstance(Conflicting.class));
        }
    }

    @Test
    void rejectsMultipleCustomAnnotations() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "first");
            binder.bindAnnotationInjector(Other.class, (context, field, annotation) -> "second");
        })) {
            assertThrows(IllegalStateException.class, () -> app.getInstance(Multiple.class));
        }
    }

    @Test
    void rejectsFinalField() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "value");
        })) {
            assertThrows(IllegalStateException.class, () -> app.getInstance(Immutable.class));
        }
    }

    @Test
    void rejectsStaticField() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> "value");
        })) {
            assertThrows(IllegalStateException.class, () -> app.getInstance(StaticField.class));
        }
    }

    @Test
    void rejectsIncompatibleResult() throws Exception {
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> 42);
        })) {
            assertThrows(IllegalStateException.class, () -> app.getInstance(Parent.class));
        }
    }

    @Test
    void retainsResolverFailureAndDoesNotInitializeBean() throws Exception {
        IllegalArgumentException failure = new IllegalArgumentException("resolver failed");
        try (AppContext app = Hasor.create().registerShutdownHook(false).build(binder -> {
            binder.bindAnnotationInjector(Custom.class, (context, field, annotation) -> { throw failure; });
        })) {
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> app.getInstance(Client.class));
            assertSame(failure, error.getCause());
        }
    }
}
