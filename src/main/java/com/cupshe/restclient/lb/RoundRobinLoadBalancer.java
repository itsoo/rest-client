package com.cupshe.restclient.lb;

import com.cupshe.restclient.exception.NotFoundException;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-Robin (RR)
 *
 * @author zxy
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger i = new AtomicInteger(-1);

    private final List<String> services;

    public RoundRobinLoadBalancer(@NonNull List<String> services) {
        this.services = services;
    }

    @Override
    public String next() {
        int curr, next, size = services.size();

        for (int j = 0; j < size; j++) {
            do {
                next = curr = i.get();
                next = ++next >= size ? 0 : next;
            } while (!i.compareAndSet(curr, next));

            String result = services.get(next);
            if (result != null) {
                return result;
            }
        }

        throw new NotFoundException();
    }
}
