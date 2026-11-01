/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.test.web.invoker;
import net.hasor.web.Invoker;
import net.hasor.web.InvokerCreator;
import org.mockito.Mockito;

public class TestInvokerCreater implements InvokerCreator {
    @Override
    public Invoker createExt(Invoker invoker) {
        TestInvoker2 inv = Mockito.mock(TestInvoker2.class);
        Mockito.when(inv.hello()).thenReturn("hello");
        Mockito.when(inv.word()).thenReturn("word");
        return inv;
    }
}
