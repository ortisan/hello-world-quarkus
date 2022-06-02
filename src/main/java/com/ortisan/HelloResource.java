package com.ortisan;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import javax.inject.Inject;

public class HelloResource {

    private final Vertx vertx;
    private final WebClient client;

    @Inject
    public HelloResource(Vertx vertx) {
        this.vertx = vertx;
        this.client = WebClient.create(vertx);
    }

    @RouteFilter(100)
    void infoMiddleware(RoutingContext rc) {
        rc.response().putHeader("X-Header", "intercepting the request");
        rc.next();
    }

    @Route(type = Route.HandlerType.FAILURE, order = 0)
    void errorHandler(Exception e, HttpServerResponse response) {
        response.setStatusCode(501).end(e.getMessage());
    }

    @Route(path = "/hello", order = 1)
    public Uni<Void> hello(RoutingContext rc) {
        final io.vertx.mutiny.ext.web.RoutingContext context = new io.vertx.mutiny.ext.web.RoutingContext(rc);
        return Uni.createFrom().item(context).onItem().transformToUni(routingContext -> {
            String url = "http://localhost:1080/assets/indices";
            return client.getAbs(url).timeout(1000).send().onItem().transform(bufferHttpResponse -> {
                routingContext.response().setStatusCode(bufferHttpResponse.statusCode());
                routingContext.response().headers().addAll(bufferHttpResponse.headers());
                routingContext.endAndForget(bufferHttpResponse.bodyAsBuffer());
                return null;
            });
        });
    }
}
