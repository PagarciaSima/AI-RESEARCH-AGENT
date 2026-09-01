package com.pgs.ai.research.agent.config;

import java.net.Proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(prefix = "proxy")
@Getter
@Setter
@Slf4j
public class ProxyConfig {

    private String host;
    private int port;
    private String username;
    private String password;
    private String userAgent;
    
    public Proxy toProxy() {
        return new Proxy(Proxy.Type.HTTP, new java.net.InetSocketAddress(host, port));
    }
}
