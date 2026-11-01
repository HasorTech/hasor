/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.web.invoker;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.concurrent.future.BasicFuture;
import net.hasor.cobble.logging.Logger;
import net.hasor.cobble.logging.LoggerFactory;
import net.hasor.web.*;
import net.hasor.web.binder.ExceptionDef;
import net.hasor.web.binder.FilterDef;
import net.hasor.web.render.OwnedResponse;
import net.hasor.web.render.RenderProcessor;

/**
 * 负责解析参数并执行调用。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2014年8月27日
 */
class InvokerCaller extends InvokerCallerParamsBuilder implements ExecuteCaller {
    static final     String               ASYNC_FAILURE_ATTRIBUTE = InvokerCaller.class.getName() + ".failure";
    protected static Logger               logger          = LoggerFactory.getLogger(InvokerCaller.class);
    private          FilterDef[]          filterArrays    = null;
    private          Supplier<Invoker>    invokerSupplier = null;
    private final    HandlerInterceptor[] interceptors;
    private final    ExceptionDef<?>[]    exceptionHandlers;
    private final    RenderProcessor      renderProcessor;
    private final    ServletVersion       servletVersion;

    public InvokerCaller(Supplier<Invoker> invokerSupplier, FilterDef[] filterArrays, HandlerInterceptor[] interceptors, //
            ExceptionDef<?>[] exceptionHandlers, RenderProcessor renderProcessor, ServletVersion servletVersion) {
        this.interceptors = interceptors;
        this.exceptionHandlers = exceptionHandlers;
        this.renderProcessor = renderProcessor;
        this.servletVersion = servletVersion;
        this.invokerSupplier = invokerSupplier;
        this.filterArrays = (filterArrays == null) ? new FilterDef[0] : filterArrays;
    }

    /** 调用目标 */
    public Future<Object> invoke(FilterChain chain) {
        Invoker invoker = this.invokerSupplier.get();
        Mapping ownerMapping = invoker.ownerMapping();
        HttpServletRequest httpRequest = invoker.getHttpRequest();
        Method targetMethod = ownerMapping.findMethod(httpRequest);

        // .异步调用
        final BasicFuture<Object> future = new BasicFuture<>();
        boolean needAsync = ownerMapping.isAsync(httpRequest);
        ServletVersion version = this.servletVersion;
        if (version.ge(ServletVersion.V3_0) && needAsync) {
            // .必须满足: Servlet3.x、环境支持异步Servlet、目标开启了Servlet3
            AsyncContext asyncContext = httpRequest.startAsync(httpRequest, invoker.getHttpResponse());
            httpRequest.setAttribute(AsyncInvocationWorker.class.getName(), Boolean.TRUE);
            asyncContext.start(new AsyncInvocationWorker(asyncContext, targetMethod) {
                private Object result;
                private Throwable failure;

                public void doWork(Method targetMethod) throws Throwable {
                    this.result = HttpParameters.executeWorker(invoker, () -> invoke(targetMethod, invoker, true));
                }

                @Override
                public void doWorkWhenError(Method targetMethod, Throwable e) {
                    httpRequest.setAttribute(ASYNC_FAILURE_ATTRIBUTE, e);
                    this.failure = e;
                }

                @Override
                protected void finish(boolean success) {
                    try {
                        super.finish(success);
                    } catch (RuntimeException | Error e) {
                        future.failed(e);
                        throw e;
                    }
                    if (success) {
                        future.completed(this.result);
                    } else {
                        future.failed(this.failure);
                    }
                }
            });
            return future;
        }

        // .同步调用
        try {
            Object invoke = invoke(targetMethod, invoker, false);
            future.completed(invoke);
        } catch (Throwable e) {
            future.failed(e);
        }
        return future;
    }

    /** 执行调用 */
    private Object invoke(final Method targetMethod, final Invoker invoker, boolean frameworkAsync) throws Throwable {
        // .准备过滤器链
        final InvokerChain ic = i -> {
            Object targetObject = i.getAppContext().getInstance(i.ownerMapping().getTargetType());
            if (targetObject == null) {
                throw new NullPointerException("mappingToDefine newInstance is null.");
            }
            if (targetObject instanceof Controller controller) {
                controller.initController(i);
            }

            // 设置contentType
            String contentType = i.contentType();
            if (StringUtils.isNotBlank(contentType)) {
                if (!i.getHttpResponse().isCommitted()) {
                    i.getHttpResponse().setContentType(contentType);
                }
            }

            // 执行调用
            try {
                final Object[] resolveParamsArrays = this.resolveParams(i, targetMethod);
                return targetMethod.invoke(targetObject, resolveParamsArrays);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        };

        // Resolve the invocation result before deciding whether to render it.
        final InvokerChain last = i -> {
            int entered = 0;
            Throwable invocationFailure = null;
            Throwable completionFailure = null;

            try {
                Object result;
                try {
                    this.renderProcessor.initInvoker(i);

                    // MVC: preHandle
                    for (HandlerInterceptor interceptor : this.interceptors) {
                        if (!interceptor.preHandle(i)) {
                            return null;
                        }
                        entered++;
                    }

                    // MVC: call
                    result = ic.doNext(i);
                    i.put(Invoker.RETURN_DATA_KEY, result);

                    // Application-managed async owns the response and completes MVC with its async cycle.
                    if (!frameworkAsync && i.getHttpRequest().isAsyncStarted()) {
                        return result;
                    }

                    // MVC: postHandle
                    for (int index = entered - 1; index >= 0; index--) {
                        this.interceptors[index].postHandle(i, result);
                        result = i.get(Invoker.RETURN_DATA_KEY);
                    }
                } catch (Throwable e) {
                    // MVC: error
                    invocationFailure = e;
                    result = this.handleException(i, e);
                }

                // final data
                i.put(Invoker.RETURN_DATA_KEY, result);
                if (!i.isSkipRender()) {
                    this.renderProcessor.invoke(i, result);
                }
                return result;
            } catch (Throwable e) {
                if (invocationFailure != null && invocationFailure != e) {
                    e.addSuppressed(invocationFailure);
                }
                completionFailure = e;
                throw e;
            } finally {
                if (completionFailure == null && !frameworkAsync && i.getHttpRequest().isAsyncStarted()) {
                    this.deferCompletion(i, entered);
                } else {
                    this.afterCompletion(i, entered, completionFailure);
                }
            }
        };

        return new InvokerChainInvocation(this.filterArrays, last).doNext(invoker);
    }

    private void deferCompletion(Invoker invoker, int entered) {
        invoker.getHttpRequest().getAsyncContext().addListener(new AsyncListener() {
            private final AtomicBoolean completed = new AtomicBoolean();
            private final AtomicReference<Throwable> failure = new AtomicReference<>();

            @Override
            public void onComplete(AsyncEvent event) {
                if (this.completed.compareAndSet(false, true)) {
                    afterCompletion(invoker, entered, this.failure.get());
                }
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                this.failure.compareAndSet(null, new TimeoutException("Asynchronous request timed out"));
            }

            @Override
            public void onError(AsyncEvent event) {
                this.failure.compareAndSet(null, event.getThrowable());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                event.getAsyncContext().addListener(this);
            }
        });
    }

    private void afterCompletion(Invoker invoker, int entered, Throwable failure) {
        for (int index = entered - 1; index >= 0; index--) {
            try {
                this.interceptors[index].afterCompletion(invoker, failure);
            } catch (Throwable e) {
                logger.error("MVC interceptor completion failed", e);
            }
        }
    }

    private Object handleException(Invoker invoker, Throwable e) throws Throwable {
        HttpServletResponse response = invoker.getHttpResponse();
        if (response.isCommitted() || response instanceof OwnedResponse owned && owned.isOwned()) {
            throw e;
        }

        ExceptionDef<?> match = null;
        for (ExceptionDef<?> definition : this.exceptionHandlers) {
            Class<?> type = definition.getExceptionType();
            if (type.isInstance(e) && (match == null || match.getExceptionType().isAssignableFrom(type))) {
                match = definition;
            }
        }

        if (match == null) {
            throw e;
        }

        Object result = match.handleException(invoker, e);
        if (result == null) {
            throw e;
        }
        return result;
    }
}
