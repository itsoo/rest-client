package com.cupshe.restclient;

import com.cupshe.ak.common.BaseConstant;
import com.cupshe.ak.request.RequestTraceIdUtils;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AsyncRequestTemplate {

    public static <T> CompletableFuture<T> asyncSupplied(Supplier<T> supplier, AsyncTaskExecutor executor) {
        String traceId = RequestTraceIdUtils.genericTraceId();
        RequestAttributes rqs = RequestContextHolder.getRequestAttributes();
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture.supplyAsync(() -> {
            try {
                MDC.put(BaseConstant.MDC_SESSION_KEY, traceId);
                RequestContextHolder.setRequestAttributes(rqs);
                // callback
                T r = supplier.get();
                log.info("Async request-callback complete");
                result.complete(r);
                return r;
            } catch (Exception e) {
                log.error("Async request-callback error", e);
                result.completeExceptionally(e);
                return null;
            } finally {
                MDC.remove(BaseConstant.MDC_SESSION_KEY);
                RequestContextHolder.resetRequestAttributes();
            }
        }, executor);

        return result;
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
