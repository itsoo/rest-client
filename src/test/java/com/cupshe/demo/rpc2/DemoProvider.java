package com.cupshe.demo.rpc2;

/**
 * DemoProvider
 *
 * @author zxy
 */

import com.cupshe.restclient.lang.RestClient;

@RestClient(name = "comment", path = "/api/v1/comment", maxAutoRetries = 3)
public interface DemoProvider {
}
