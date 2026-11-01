/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.config;
import java.util.List;
import javax.servlet.ServletContext;
import net.hasor.cobble.setting.Settings;
import net.hasor.config.web.cors.CorsFilter;
import net.hasor.config.webconfig.WebMvcConfiguration;
import net.hasor.core.AppContext;
import net.hasor.core.BindInfo;
import net.hasor.core.Hasor;
import net.hasor.web.InvokerFilter;
import net.hasor.web.HandlerInterceptor;
import net.hasor.web.binder.FilterDef;
import net.hasor.web.binder.RenderDef;
import net.hasor.web.render.json.JsonRenderEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebMvcConfigurerTest {
    @Test
    public void configurationShouldRegisterWebMvcComponentsDuringModuleLoading() throws Throwable {
        WebMvcConfiguration.RESOURCE_CONFIGURES.set(0);
        WebMvcConfiguration.CORS_CONFIGURES.set(0);
        WebMvcConfiguration.JSON_CONFIGURES.set(0);
        WebMvcConfiguration.INTERCEPTOR_CONFIGURES.set(0);

        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        Mockito.when(servletContext.getContextPath()).thenReturn("/");
        Mockito.when(servletContext.getEffectiveMajorVersion()).thenReturn(3);
        Mockito.when(servletContext.getVirtualServerName()).thenReturn("test");

        AppContext context = Hasor.create(servletContext)//
                .addSettings(Settings.DefaultNameSpace, "hasor.loadPackages", "net.hasor.config.webconfig")//
                .build();

        assertEquals(1, WebMvcConfiguration.RESOURCE_CONFIGURES.get());
        assertEquals(1, WebMvcConfiguration.CORS_CONFIGURES.get());
        assertEquals(1, WebMvcConfiguration.JSON_CONFIGURES.get());
        assertEquals(1, WebMvcConfiguration.INTERCEPTOR_CONFIGURES.get());
        HandlerInterceptor[] interceptors = context.getInstance(HandlerInterceptor[].class);
        assertEquals(2, interceptors.length);
        assertSame(WebMvcConfiguration.FIRST_INTERCEPTOR, interceptors[0]);
        assertSame(WebMvcConfiguration.SECOND_INTERCEPTOR, interceptors[1]);

        List<FilterDef> filters = context.findBindingBean(FilterDef.class);
        assertEquals(1, filters.size());
        assertTrue(filters.stream().map(filter -> newFilter(context, filter)).anyMatch(CorsFilter.class::isInstance));

        List<RenderDef> renders = context.findBindingBean(RenderDef.class).stream().filter(def -> !def.isFallback()).toList();
        assertEquals(1, renders.size());
        assertEquals("json", renders.get(0).getRenderName());
        assertTrue(renders.get(0).newEngine(context) instanceof JsonRenderEngine);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static InvokerFilter newFilter(AppContext context, FilterDef filter) {
        return (InvokerFilter) context.getInstance((BindInfo) filter.getTargetType());
    }
}
