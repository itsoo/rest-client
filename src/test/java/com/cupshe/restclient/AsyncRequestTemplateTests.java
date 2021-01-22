package com.cupshe.restclient;

import com.cupshe.Application;
import com.cupshe.ak.ResponseVO;
import com.cupshe.demo.domain.DemoDTO;
import com.cupshe.demo.service.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AsyncRequestTemplateTests
 *
 * @author zxy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
public class AsyncRequestTemplateTests {

    @Autowired
    private DemoService demoService;

    @Autowired
    private ThreadPoolTaskExecutor dealResourceExecutor;

    @Test
    public void testAsyncSupplied() throws Exception {
        CompletableFuture<ResponseVO<?>> future = AsyncRequestTemplate.asyncSupplied(() -> {
            DemoDTO dto = DemoDTO.defaultInstance();
            return demoService.postForm(dto);
        }, dealResourceExecutor);

        System.out.println(future.get());
    }

    @Bean
    public ThreadPoolTaskExecutor dealResourceExecutor() {
        ThreadPoolTaskExecutor result = new ThreadPoolTaskExecutor();
        result.setCorePoolSize(10);
        result.setMaxPoolSize(10);
        result.setQueueCapacity(500);
        result.setKeepAliveSeconds(300);
        result.setWaitForTasksToCompleteOnShutdown(true);
        result.setThreadNamePrefix("deal-resource-thread-pool-");
        result.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        result.setAwaitTerminationSeconds(60);
        return result;
    }
}
