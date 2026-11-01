/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.web.render;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.AppContext;
import net.hasor.web.AbstractTest;
import net.hasor.web.Invoker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RenderBasicTest extends AbstractTest {
    @Test
    public void renderInvoker_params() throws Throwable {
        //
        HttpServletRequest request = mockRequest("get", new URL("http://www.hasor.net/abc.do?a=1&b=2"));
        Invoker invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getHttpRequest()).thenReturn(request);
        AppContext appContext = Mockito.mock(AppContext.class);
        Settings settings = Mockito.mock(Settings.class);
        Mockito.when(invoker.getAppContext()).thenReturn(appContext);
        Mockito.when(appContext.getSettings()).thenReturn(settings);
        RenderInvokerSupplier supplier = new RenderInvokerSupplier(invoker);
        //
        assert supplier.get("req_a").equals("1");
        assert supplier.get("req_b").equals("2");
    }
}
