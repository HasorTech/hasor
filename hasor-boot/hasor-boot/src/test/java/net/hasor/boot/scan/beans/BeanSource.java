/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.boot.scan.beans;
import net.hasor.config.Bean;
import net.hasor.core.Inject;

public class BeanSource {
    @Bean
    public static class Repository {
        public String message() {
            return "scanned";
        }
    }

    @Bean
    public static class Service {
        @Inject
        private Repository repository;

        public String message() {
            return this.repository.message();
        }
    }
}
