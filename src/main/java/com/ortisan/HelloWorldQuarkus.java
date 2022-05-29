package com.ortisan;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

import javax.inject.Inject;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class HelloWorldQuarkus {

    private final Vertx vertx;
    private final WebClient client;

    @Inject
    public HelloWorldQuarkus(Vertx vertx) {
        this.vertx = vertx;
        this.client = WebClient.create(vertx);
    }

    @RouteFilter(100)
    void infoMiddleware(RoutingContext rc) {
        rc.response().putHeader("X-Header", "intercepting the request");
        rc.next();
    }

    @Route(type = Route.HandlerType.FAILURE)
    void errorHandler(Exception e, HttpServerResponse response) {
        response.setStatusCode(501).end(e.getMessage());
    }

    @Route(path = "/hello")
    public Uni<io.vertx.mutiny.ext.web.RoutingContext> hello(RoutingContext rc) {
        final io.vertx.mutiny.ext.web.RoutingContext context = new io.vertx.mutiny.ext.web.RoutingContext(rc);
        String url = "http://localhost:1080/assets/indices";
        return client.getAbs(url)
                .timeout(1000)
                .send()
                .onItem()
                .transform(bufferHttpResponse -> {
                    context.response().setStatusCode(bufferHttpResponse.statusCode());
                    context.response().headers().addAll(bufferHttpResponse.headers());
                    context.endAndForget(bufferHttpResponse.bodyAsBuffer());
                    return context;
                })
                .onFailure()
                .transform(throwable -> {
                    context.response().setStatusCode(500);
                    context.response().headers().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
                    context.end(throwable.getMessage());
                    return throwable;
                });

        // client.getAbs(url).timeout(10).send().onItem(bufferHttpResponse -> {
        // if (true) {
        // throw new RuntimeException("Teste");
        // }
        // rc.response().setStatusCode(bufferHttpResponse.statusCode());
        // rc.end(bufferHttpResponse.bodyAsBuffer());
        // });
        // .onFailure(throwable -> {
        // rc.response().setStatusCode(500);
        // rc.end(throwable.getMessage());
        // });
    }
}
