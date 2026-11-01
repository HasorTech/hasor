/*
 * Copyright 2015-2022 the original author or authors.
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.core.event;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.core.EventCallBackHook;
import net.hasor.core.EventListener;
import net.hasor.core.FireType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsyncEventTest {
    @Test
    @Timeout(30)
    public void asyncEventTest() throws InterruptedException {
        // 准备独立事件管理器，以两个同时进入的监听器验证并发。
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        StandardEventManager ec = new StandardEventManager(20, "TestEvent", classLoader);
        CountDownLatch concurrentEvents = new CountDownLatch(2);
        CountDownLatch releaseEvents = new CountDownLatch(1);
        try {
            String eventName = "MyEvent";
            int eventCount = 50;
            CountDownLatch completedEvents = new CountDownLatch(eventCount);
            Set<String> receivedEvents = new CopyOnWriteArraySet<>();
            ec.addListener(eventName, (event, eventData) -> {
                concurrentEvents.countDown();
                releaseEvents.await();
                receivedEvents.add(event + eventData);
                completedEvents.countDown();
            });

            // 提交全部事件，在监听器尚未结束时确认另一个监听器也能开始。
            Set<String> expectedEvents = new HashSet<>();
            for (int i = 0; i < eventCount; i++) {
                expectedEvents.add(eventName + i);
                ec.fireAsyncEvent(eventName, i);
            }
            assertTrue(concurrentEvents.await(10, TimeUnit.SECONDS), "Event listeners must execute concurrently");

            // 放行监听器并等待结果，避免以墙钟耗时推断并发正确性。
            releaseEvents.countDown();
            assertTrue(completedEvents.await(10, TimeUnit.SECONDS), "All event listeners must finish");
            assertEquals(expectedEvents, receivedEvents);
        } finally {
            releaseEvents.countDown();
            ec.release();
        }
    }

    @Test
    @Timeout(30)
    public void onesAsyncEventTest() throws InterruptedException {
        // 阻塞监听器，验证批量提交不会等待监听器完成。
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        StandardEventManager ec = new StandardEventManager(10, "TestEvent", classLoader);
        CountDownLatch releaseEvents = new CountDownLatch(1);
        try {
            String eventName = "MyEvent";
            int eventCount = 50;
            CountDownLatch startedEvent = new CountDownLatch(1);
            CountDownLatch completedEvents = new CountDownLatch(eventCount);
            Set<String> receivedEvents = new CopyOnWriteArraySet<>();
            ec.addListener(eventName, (event, eventData) -> {
                startedEvent.countDown();
                releaseEvents.await();
                receivedEvents.add(event + eventData);
                completedEvents.countDown();
            });

            // 全部提交返回后，至少一个监听器已进入，但仍在等待放行。
            for (int i = 0; i < eventCount; i++) {
                ec.fireAsyncEvent(eventName, i);
            }
            assertTrue(startedEvent.await(10, TimeUnit.SECONDS), "An event listener must start");
            assertTrue(receivedEvents.isEmpty());

            // 放行后按完成信号等待，不依赖固定睡眠。
            releaseEvents.countDown();
            assertTrue(completedEvents.await(10, TimeUnit.SECONDS), "All event listeners must finish");
            assertEquals(eventCount, receivedEvents.size());
        } finally {
            releaseEvents.countDown();
            ec.release();
        }
    }

    @Test
    @Timeout(30)
    public void syncTest() throws InterruptedException {
        // 注册正常与异常监听器，分别记录完成和异常回调。
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        StandardEventManager ec = new StandardEventManager(20, "TestEvent", classLoader);
        try {
            AtomicInteger exceptionInteger = new AtomicInteger();
            AtomicInteger completeInteger = new AtomicInteger();
            CountDownLatch callbacks = new CountDownLatch(2);
            assertTrue(ec.addListener("EventA", (EventListener<Object>) (event, eventData) -> {
            }));
            assertTrue(ec.addListener("EventB", (EventListener<Object>) (event, eventData) -> {
                throw new Exception("1111");
            }));
            EventCallBackHook<Object> callBack = new EventCallBackHook<Object>() {
                @Override
                public void handleException(String eventType, Object eventData, Throwable e) {
                    exceptionInteger.incrementAndGet();
                    callbacks.countDown();
                }

                @Override
                public void handleComplete(String eventType, Object eventData) {
                    completeInteger.incrementAndGet();
                    callbacks.countDown();
                }
            };

            // 等待两条实际回调后，再核对各自的执行次数。
            ec.fireAsyncEvent("EventA", null, FireType.Continue, callBack);
            ec.fireAsyncEvent("EventB", null, FireType.Continue, callBack);
            assertTrue(callbacks.await(10, TimeUnit.SECONDS), "Both event callbacks must finish");
            assertEquals(1, completeInteger.get());
            assertEquals(1, exceptionInteger.get());
        } finally {
            ec.release();
        }
    }
}
