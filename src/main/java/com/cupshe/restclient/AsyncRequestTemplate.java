package com.cupshe.restclient;

import com.cupshe.ak.common.BaseConstant;
import com.cupshe.ak.request.RequestTraceIdUtils;
import org.slf4j.MDC;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * AsyncRequestTemplate
 *
 * @author zxy
 */
public class AsyncRequestTemplate {

    public static void asyncCallback(Supplier<?> supplier, AsyncTaskExecutor executor) {
        String traceId = RequestTraceIdUtils.genericTraceId();
        RequestAttributes rqs = RequestContextHolder.getRequestAttributes();

        CompletableFuture.supplyAsync(() -> {
            try {
                MDC.put(BaseConstant.MDC_SESSION_KEY, traceId);
                RequestContextHolder.setRequestAttributes(rqs);
                return supplier.get();
            } finally {
                MDC.remove(BaseConstant.MDC_SESSION_KEY);
                RequestContextHolder.resetRequestAttributes();
            }
        }, executor);
    }

    public static Future<?> asyncCallback(Callable<?> callable, ExecutorService executor) {
        String traceId = RequestTraceIdUtils.genericTraceId();
        RequestAttributes rqs = RequestContextHolder.getRequestAttributes();

        return executor.submit(() -> {
            try {
                MDC.put(BaseConstant.MDC_SESSION_KEY, traceId);
                RequestContextHolder.setRequestAttributes(rqs);
                return callable.call();
            } finally {
                MDC.remove(BaseConstant.MDC_SESSION_KEY);
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }
}
