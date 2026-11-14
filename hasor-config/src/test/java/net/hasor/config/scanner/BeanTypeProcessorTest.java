/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.config.scanner;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.cobble.setting.Settings;
import net.hasor.config.Bean;
import net.hasor.core.AppContext;
import net.hasor.core.DependsOn;
import net.hasor.core.Hasor;
import net.hasor.core.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeanTypeProcessorTest {
    @Test
    public void registersNamedSingletonWithInjectionAndLifecycle() throws Exception {
        ManagedService.initialized.set(0);
        ManagedService.destroyed.set(0);
        try (AppContext context = manualHasor().build(binder -> {
            new BeanTypeProcessor().process(binder, List.of(ManagedService.class, Repository.class));
        })) {
            ManagedService service = context.getInstance(ManagedService.class);
            assertSame(service, context.getInstance("managedService"));
            assertSame(context.getInstance(Repository.class), service.repository);
            assertTrue(service.ready);
            assertEquals(1, ManagedService.initialized.get());
        }
        assertEquals(1, ManagedService.destroyed.get());
    }

    @Test
    public void prototypeCreatesIndependentInstancesWithDefaultName() throws Exception {
        try (AppContext context = manualHasor().build(binder -> {
            new BeanTypeProcessor().process(binder, List.of(PrototypeBean.class));
        })) {
            assertNotSame(context.getInstance(PrototypeBean.class), context.getInstance(PrototypeBean.class));
            assertNotNull(context.getInstance("prototypeBean"));
        }
    }

    @Test
    public void repeatedScanKeepsOneBindingAndExplicitBindingsTakePrecedence() throws Exception {
        Repository explicit = new Repository();
        try (AppContext context = manualHasor().build(binder -> {
            binder.bindType(Repository.class).toInstance(explicit);
            BeanTypeProcessor processor = new BeanTypeProcessor();
            processor.process(binder, List.of(Repository.class, PrototypeBean.class));
            processor.process(binder, List.of(Repository.class, PrototypeBean.class));
        })) {
            assertSame(explicit, context.getInstance(Repository.class));
            assertEquals(1, context.findBindingRegister(Repository.class).size());
            assertEquals(1, context.findBindingRegister(PrototypeBean.class).size());
        }
    }

    @Test
    public void explicitNameResolvesToTheSameSingletonByIdAndName() throws Exception {
        try (AppContext context = manualHasor().build(binder -> {
            new BeanTypeProcessor().process(binder, List.of(FirstNamed.class));
        })) {
            assertSame(context.getInstance("duplicate"), context.findBindingBean("duplicate", FirstNamed.class));
        }
    }

    @Test
    public void conflictingNamesFailDuringRegistration() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> manualHasor().build(binder -> {
            new BeanTypeProcessor().process(binder, List.of(FirstNamed.class, SecondNamed.class));
        }));
        assertTrue(failure.getMessage().contains("Conflicting @Bean name 'duplicate'"));
    }

    @Test
    public void abstractBeanFailsDuringRegistration() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> manualHasor().build(binder -> {
            new BeanTypeProcessor().process(binder, List.of(AbstractBean.class));
        }));
        assertTrue(failure.getMessage().contains(AbstractBean.class.getName()));
    }

    private Hasor manualHasor() {
        return Hasor.create().addSettings(Settings.DefaultNameSpace, "hasor.loadPackages", "example.no_autoscan");
    }

    @Bean
    public static class Repository {
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn(types = Repository.class)
    public static class ManagedService {
        static final AtomicInteger initialized = new AtomicInteger();
        static final AtomicInteger destroyed   = new AtomicInteger();
        @Inject
        Repository repository;
        boolean ready;

        public void start() {
            ready = repository != null;
            initialized.incrementAndGet();
        }

        public void stop() {
            destroyed.incrementAndGet();
        }
    }

    @Bean(singleton = false)
    public static class PrototypeBean {
    }

    @Bean("duplicate")
    public static class FirstNamed {
    }

    @Bean("duplicate")
    public static class SecondNamed {
    }

    @Bean
    public abstract static class AbstractBean {
    }
}
