package com.cupshe.restclient.lb;

import com.cupshe.restclient.exception.NotFoundException;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random (R)
 *
 * @author zxy
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final List<String> services;

    public RandomLoadBalancer(@NonNull List<String> services) {
        this.services = services;
    }

    @Override
    public String next() {
        int next, size = services.size();

        for (int j = 0; j < size; j++) {
            next = ThreadLocalRandom.current().nextInt(0, size);
            String result = services.get(next);
            if (result != null) {
                return result;
            }
        }

        throw new NotFoundException();
    }
}
