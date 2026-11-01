/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.web;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.hasor.cobble.CollectionUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.concurrent.future.BasicFuture;
import net.hasor.cobble.io.output.WriterOutputStream;
import net.hasor.cobble.setting.SettingNode;
import net.hasor.cobble.setting.data.TreeNode;
import net.hasor.core.AppContext;
import net.hasor.core.BindInfo;
import net.hasor.core.Hasor;
import net.hasor.web.binder.OneConfig;
import net.hasor.web.invoker.ExecuteCaller;
import net.hasor.web.invoker.InvokerContext;
import org.mockito.stubbing.Answer;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2016-12-16
 */
public class AbstractTest {
    //
    public enum LoadModule {
        Web,
        Render,
        Valid
    }

    public interface BuildHasor {
        Hasor build(Object context);
    }

    protected <T> BindInfo<T> bindInfo(Class<T> tClass) {
        return bindInfo(UUID.randomUUID().toString().replace("-", ""), tClass);
    }

    protected <T> BindInfo<T> bindInfo(String bindID, Class<T> tClass) {
        BindInfo targetType = Mockito.mock(BindInfo.class);
        Mockito.when(targetType.getBindID()).thenReturn(bindID);
        Mockito.when(targetType.getBindType()).thenReturn(tClass);
        return targetType;
    }

    /** Mock 2.4 */
    protected ServletContext servlet24(final String context) {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenThrow(new UnsupportedOperationException());
        return servletContext;
    }

    protected ServletContext basicServlet(ServletContext servletContext) {
        Mockito.when(servletContext.getRealPath(anyString())).then(invocation -> {
            return new File(invocation.getArguments()[0].toString()).getAbsolutePath();
        });
        //
        final Map<String, Object> attrMap = new HashMap<>();
        Mockito.when(servletContext.getAttributeNames()).thenAnswer((Answer<Enumeration<String>>) invocation -> {
            return CollectionUtils.asEnumeration(attrMap.keySet().iterator());
        });
        Mockito.when(servletContext.getAttribute(anyString())).thenAnswer(invocation -> {
            return attrMap.get(invocation.getArguments()[0]);
        });
        Mockito.doAnswer(invocation -> {
            return attrMap.put((String) invocation.getArguments()[0], invocation.getArguments()[1]);
        }).when(servletContext).setAttribute(anyString(), any());
        Mockito.doAnswer(invocation -> {
            return attrMap.remove(invocation.getArguments()[0]);
        }).when(servletContext).removeAttribute(anyString());
        //
        return servletContext;
    }

    protected ServletContext servletInitParams(final ServletContext servletContext, Map<String, String> contextParams) {
        Mockito.when(servletContext.getInitParameterNames())//
                .thenReturn(CollectionUtils.asEnumeration(contextParams.keySet().iterator()));
        Mockito.when(servletContext.getInitParameter(anyString())).then((Answer<String>) invocation -> {
            return contextParams.get(invocation.getArguments()[0].toString());
        });
        return basicServlet(servletContext);
    }

    /** Mock 2.5 */
    protected ServletContext servlet25(final String context) {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn(context);
        Mockito.when(servletContext.getEffectiveMajorVersion()).thenThrow(new UnsupportedOperationException());
        Mockito.when(servletContext.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        return basicServlet(servletContext);
    }

    /** Mock 3.0 */
    protected ServletContext servlet30(final String context) {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn(context);
        Mockito.when(servletContext.getEffectiveMajorVersion()).thenReturn(123);
        Mockito.when(servletContext.getVirtualServerName()).thenThrow(new UnsupportedOperationException());
        Mockito.when(servletContext.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        return basicServlet(servletContext);
    }

    /** Mock 3.1 */
    protected ServletContext servlet31(final String context) {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn(context);
        Mockito.when(servletContext.getEffectiveMajorVersion()).thenReturn(123);
        Mockito.when(servletContext.getVirtualServerName()).thenReturn("abc");
        Mockito.when(servletContext.getClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        return basicServlet(servletContext);
    }

    private SettingNode defaultInvokerCreaterSetXmlNode(LoadModule... modules) {
        TreeNode xmlNode = new TreeNode("invokerCreatorSet");
        List<LoadModule> moduleSet = Arrays.asList(modules);
        if (moduleSet.contains(LoadModule.Valid)) {
            TreeNode validInvoker = xmlNode.newNode("invokerCreator");
            validInvoker.setValue("type", "net.hasor.web.valid.ValidInvoker");
            validInvoker.setValue("net.hasor.web.valid.ValidInvokerCreator");
        }
        if (moduleSet.contains(LoadModule.Render)) {
            TreeNode renderInvoker = xmlNode.newNode("invokerCreator");
            renderInvoker.setValue("type", "net.hasor.web.render.RenderInvoker");
            renderInvoker.setValue("net.hasor.web.render.RenderInvokerCreator");
        }
        return xmlNode;
    }

    private SettingNode defaultInnerApiBinderSetXmlNode(LoadModule... modules) {
        TreeNode xmlNode = new TreeNode("innerApiBinderSet");
        List<LoadModule> moduleSet = Arrays.asList(modules);
        if (moduleSet.contains(LoadModule.Web)) {
            TreeNode webBinder = xmlNode.newNode("binder");
            webBinder.setValue("type", "net.hasor.web.WebApiBinder");
            webBinder.setValue("net.hasor.web.binder.InvokerWebApiBinderCreator");
        }
        return xmlNode;
    }

    protected AppContext buildWebAppContext(WebModule webModule, ServletContext servletContext, LoadModule... modules) {
        return buildWebAppContext(null, Hasor::create, webModule, servletContext, modules);
    }

    protected AppContext buildWebAppContext(String mainconfig, WebModule webModule, ServletContext servletContext, LoadModule... modules) {
        return buildWebAppContext(mainconfig, Hasor::create, webModule, servletContext, modules);
    }

    protected AppContext buildWebAppContext(BuildHasor buildHasor, WebModule webModule, ServletContext servletContext, LoadModule... modules) {
        return buildWebAppContext(null, Hasor::create, webModule, servletContext, modules);
    }

    protected AppContext buildWebAppContext(String mainconfig, BuildHasor buildHasor, WebModule webModule, ServletContext servletContext, LoadModule... modules) {
        Hasor settings = buildHasor.build(servletContext)//
                .addSettings("http://test.hasor.net", "hasor.innerApiBinderSet", defaultInnerApiBinderSetXmlNode(modules))//
                .addSettings("http://test.hasor.net", "hasor.invokerCreatorSet", defaultInvokerCreaterSetXmlNode(modules))//
                .mainSettingWith(mainconfig)//
                .addModules(webModule);
        return settings.build();
    }

    private String singleObjectFormMap(Map headerMap, String name) {
        Object[] objectFormMap = multipleObjectFormMap(headerMap, name);
        if (objectFormMap == null || objectFormMap.length == 0) {
            return null;
        } else {
            return objectFormMap[0].toString();
        }
    }

    private String[] multipleObjectFormMap(Map headerMap, String name) {
        if (headerMap == null) {
            return null;
        }
        Object o = headerMap.get(name);
        if (o == null) {
            return null;
        }
        if (o instanceof List) {
            return (String[]) ((List) o).toArray(new String[0]);
        }
        if (o.getClass().isArray()) {
            Object[] arrayData = (Object[]) o;
            String[] arrayStr = new String[arrayData.length];
            for (int i = 0; i < arrayData.length; i++) {
                arrayStr[i] = (String) arrayData[i];
            }
            return arrayStr;
        }
        return new String[] { o.toString() };
    }

    protected HttpServletRequest mockRequest(final String httpMethod, URL requestURL) {
        return mockRequest(httpMethod, requestURL, null, null, null);
    }

    protected HttpServletRequest mockRequest(final String httpMethod, URL requestURL, Map<String, String[]> headerMap, Cookie[] cookies, final Map<String, String> postParams) {
        //
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn(httpMethod);
        Mockito.when(request.getCharacterEncoding()).thenReturn("utf-8");
        //
        Mockito.when(request.getRequestURI()).thenReturn(requestURL.getPath());
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(requestURL.getPath()));
        Mockito.when(request.getQueryString()).thenReturn(requestURL.getQuery());
        Mockito.when(request.getContextPath()).thenReturn("");
        Mockito.when(request.getProtocol()).thenReturn(requestURL.getProtocol());
        Mockito.when(request.getLocalName()).thenReturn(requestURL.getHost());
        Mockito.when(request.getLocalAddr()).thenReturn("127.0.0.1");
        int port = requestURL.getPort();
        Mockito.when(request.getLocalPort()).thenReturn((port == -1) ? 80 : port);
        //
        cookies = cookies == null ? new Cookie[0] : cookies;
        Mockito.when(request.getCookies()).thenReturn(cookies);
        //
        HttpSession mockSession = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession()).thenReturn(mockSession);
        Mockito.when(request.getSession(anyBoolean())).thenReturn(mockSession);
        Mockito.when(request.getRequestDispatcher(anyString())).thenReturn(Mockito.mock(RequestDispatcher.class));
        //
        Mockito.when(request.getHeader(anyString())).thenAnswer((Answer<String>) invocation -> {
            return singleObjectFormMap(headerMap, (String) invocation.getArguments()[0]);
        });
        Mockito.when(request.getHeaderNames()).thenAnswer((Answer<Enumeration<String>>) invocation -> {
            if (headerMap == null) {
                return null;
            }
            Set<String> keySet = headerMap.keySet();
            return CollectionUtils.asEnumeration(keySet.iterator());
        });
        Mockito.when(request.getHeaders(anyString())).thenAnswer((Answer<Enumeration<String>>) invocation -> {
            if (headerMap == null) {
                return null;
            }
            String[] objects = multipleObjectFormMap(headerMap, (String) invocation.getArguments()[0]);
            return CollectionUtils.asEnumeration(Arrays.asList(objects).iterator());
        });
        //
        String query = requestURL.getQuery();
        Map<String, List<String>> tmpQueryMap = new HashMap<>();
        if (postParams != null) {
            for (final String key : postParams.keySet()) {
                tmpQueryMap.put(key, new ArrayList<String>() {
                    {
                        add(postParams.get(key));
                    }
                });
            }
        }
        if (StringUtils.isNotBlank(query)) {
            String[] paramArray = query.split("&");
            for (String param : paramArray) {
                String[] kv = param.split("=");
                String key = kv[0].trim();
                String value = kv[1].trim();
                List<String> strings = tmpQueryMap.computeIfAbsent(key, k -> new ArrayList<>());
                strings.add(value);
            }
        }
        final Map<String, String[]> queryMap = new HashMap<>();
        for (String key : tmpQueryMap.keySet()) {
            queryMap.put(key, tmpQueryMap.get(key).toArray(new String[0]));
        }
        Mockito.when(request.getParameterMap()).thenAnswer((Answer<Map<String, String[]>>) invocation -> {
            return queryMap;//
        });
        Mockito.when(request.getParameterNames()).thenAnswer((Answer<Enumeration<String>>) invocation -> {
            return CollectionUtils.asEnumeration(queryMap.keySet().iterator());
        });
        Mockito.when(request.getParameterValues(anyString())).thenAnswer((Answer<String[]>) invocation -> {
            return multipleObjectFormMap(queryMap, (String) invocation.getArguments()[0]);
        });
        Mockito.when(request.getParameter(anyString())).thenAnswer((Answer<String>) invocation -> {
            String[] strings = multipleObjectFormMap(queryMap, (String) invocation.getArguments()[0]);
            return strings != null && strings.length > 0 ? strings[0] : null;
        });
        //
        final Map<String, Object> attrMap = new HashMap<>();
        Mockito.when(request.getAttributeNames()).thenAnswer((Answer<Enumeration<String>>) invocation -> {
            return CollectionUtils.asEnumeration(attrMap.keySet().iterator());
        });
        Mockito.when(request.getAttribute(anyString())).thenAnswer(invocation -> {
            return attrMap.get(invocation.getArguments()[0]);
        });
        Mockito.doAnswer(invocation -> {
            return attrMap.put((String) invocation.getArguments()[0], invocation.getArguments()[1]);
        }).when(request).setAttribute(anyString(), any());
        Mockito.doAnswer(invocation -> {
            return attrMap.remove(invocation.getArguments()[0]);
        }).when(request).removeAttribute(anyString());
        //
        AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
        Mockito.doAnswer(invocation -> {
            BasicFuture<Object> future = new BasicFuture<>();
            new Thread(() -> {
                ((Runnable) invocation.getArguments()[0]).run();
                future.completed(new Object());
            }).start();
            return future.get();
        }).when(asyncContext).start(any());
        Mockito.when(request.startAsync(any(), any())).thenReturn(asyncContext);
        Mockito.when(request.startAsync()).thenReturn(asyncContext);
        Mockito.when(request.isAsyncSupported()).thenReturn(true);
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);
        //
        return request;
    }

    protected void mockCharacterEncoding(HttpServletRequest request, HttpServletResponse response) {
        if (request != null) {
            try {
                AtomicReference<String> reference = new AtomicReference<>();
                Mockito.when(request.getCharacterEncoding()).then((Answer<String>) invocation -> reference.get());
                Mockito.doAnswer((Answer<String>) invocation -> {
                    reference.set((String) invocation.getArguments()[0]);
                    return null;
                }).when(request).setCharacterEncoding(anyString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (response != null) {
            mockRenderResponse(response);
            try {
                AtomicReference<String> reference = new AtomicReference<>();
                Mockito.when(response.getCharacterEncoding()).then((Answer<String>) invocation -> reference.get());
                Mockito.doAnswer((Answer<String>) invocation -> {
                    reference.set((String) invocation.getArguments()[0]);
                    return null;
                }).when(response).setCharacterEncoding(anyString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected String mockAndCallHttp(String httpMethod, String callURL, AppContext appContext) throws Throwable {
        return mockAndCallHttp(httpMethod, callURL, appContext, null, null);
    }

    protected String mockAndCallHttp(String httpMethod, String callURL, AppContext appContext, Set<String> responseType, Set<String> dispatcher) throws Throwable {
        HttpServletRequest httpRequest = mockRequest(httpMethod, new URL(callURL));
        HttpServletResponse httpResponse = Mockito.mock(HttpServletResponse.class);
        mockRenderResponse(httpResponse);
        AtomicReference<String> contentType = new AtomicReference<>();
        Mockito.when(httpResponse.getContentType()).thenAnswer(invocation -> contentType.get());
        StringWriter stringWriter = new StringWriter();
        Mockito.when(httpResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        Mockito.when(httpResponse.getOutputStream()).thenReturn(new DelegatingServletOutputStream(new WriterOutputStream(stringWriter)));
        //
        Mockito.doAnswer((Answer<Void>) invocation -> {
            contentType.set(invocation.getArgument(0));
            if (responseType != null) {
                responseType.add(invocation.getArguments()[0].toString());
            }
            return null;
        }).when(httpResponse).setContentType(anyString());
        //
        Mockito.when(httpRequest.getRequestDispatcher(anyString())).then(invocation -> {
            if (dispatcher != null) {
                dispatcher.add(invocation.getArguments()[0].toString());
            }
            return Mockito.mock(RequestDispatcher.class);
        });
        //
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new OneConfig("", () -> appContext));
        //
        ExecuteCaller caller = invokerContext.genCaller(httpRequest, httpResponse);
        caller.invoke(null).get();
        return stringWriter.toString();
    }

    protected Object callInvoker(AppContext appContext, HttpServletRequest request) throws Throwable {
        InvokerContext invokerContext = new InvokerContext();
        invokerContext.initContext(appContext, new OneConfig("", () -> appContext));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        mockRenderResponse(response);
        ExecuteCaller caller = invokerContext.genCaller(request, response);
        return caller.invoke(null).get();
    }

    /** Model the Servlet response contract now that plain Web renders returned values. */
    protected void mockRenderResponse(HttpServletResponse response) {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> encoding = new AtomicReference<>("UTF-8");
        Mockito.when(response.getContentType()).thenAnswer(invocation -> contentType.get());
        Mockito.doAnswer(invocation -> {
            contentType.set(invocation.getArgument(0));
            return null;
        }).when(response).setContentType(anyString());
        Mockito.when(response.getCharacterEncoding()).thenAnswer(invocation -> encoding.get());
        Mockito.doAnswer(invocation -> {
            encoding.set(invocation.getArgument(0));
            return null;
        }).when(response).setCharacterEncoding(anyString());
        try {
            Mockito.when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(new java.io.ByteArrayOutputStream()));
            Mockito.when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }
}
