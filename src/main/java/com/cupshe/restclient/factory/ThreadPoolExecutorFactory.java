package com.cupshe.restclient.factory;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolExecutorFactory
 *
 * @author zxy
 */
public class ThreadPoolExecutorFactory {

    private static final String THREAD_NAME_PREFIX = "rest-client-pool-";

    public static ThreadPoolExecutor getThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                // core-max size
                30, 200,
                // keep-alive time
                0L, TimeUnit.MILLISECONDS,
                // queue
                new LinkedBlockingQueue<>(200),
                // thread factory
                new CustomizableThreadFactory(THREAD_NAME_PREFIX),
                // rejected
                new ThreadPoolExecutor.AbortPolicy());
    }
}
