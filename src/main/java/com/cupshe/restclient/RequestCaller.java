package com.cupshe.restclient;

import com.cupshe.restclient.exception.NotFoundException;
import com.cupshe.restclient.lang.PureFunction;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cupshe.restclient.lang.RestClient.LoadBalanceType;

/**
 * RequestCaller
 *
 * @author zxy
 */
@Data
class RequestCaller {

    private String name;
    private List<String> services;
    private final AbstractCall roundRobin;
    private final AbstractCall random;

    private RequestCaller() {
        roundRobin = new CallRoundRobin();
        random = new CallRandom();
    }

    @PureFunction
    String get(LoadBalanceType loadBalanceType) {
        int i = getCall(loadBalanceType).index();
        if (i == -1) {
            throw new NotFoundException();
        }

        return services.get(i);
    }

    private AbstractCall getCall(LoadBalanceType loadBalanceType) {
        return LoadBalanceType.R.equals(loadBalanceType) ? random : roundRobin;
    }

    private abstract static class AbstractCall {
        /**
         * 获取应访问服务列表索引
         *
         * @return int
         */
        abstract int index();
    }

    private class CallRoundRobin extends AbstractCall {
        private final AtomicInteger i = new AtomicInteger(0);

        @Override
        int index() {
            if (CollectionUtils.isEmpty(services)) {
                return -1;
            }

            int curr, next, size = services.size();

            do {
                curr = i.get();
                next = curr >= size - 1 ? 0 : curr + 1;
            } while (!i.compareAndSet(curr, next));

            return curr;
        }
    }

    private class CallRandom extends AbstractCall {
        @Override
        int index() {
            return CollectionUtils.isEmpty(services) ? -1 : new Random().nextInt(services.size());
        }
    }
}
