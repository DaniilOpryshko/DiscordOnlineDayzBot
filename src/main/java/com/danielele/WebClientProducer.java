package com.danielele;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class WebClientProducer
{
    private final Vertx vertx;

    @Inject
    public WebClientProducer(Vertx vertx)
    {
        this.vertx = vertx;
    }

    @Produces
    @ApplicationScoped
    public WebClient webClient()
    {
        WebClientOptions options = new WebClientOptions()
                .setKeepAlive(true)
                .setMaxPoolSize(10)
                .setConnectTimeout(5000);

        return WebClient.create(vertx, options);
    }
}
