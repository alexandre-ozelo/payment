package com.sensedia.payment.domain.resource;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Payment extends AbstractVerticle {

  @Override
  public void start() {

    val router = Router.router(vertx);
    val healthCheck = HealthCheckHandler.create(vertx);
    val httpServer = this.vertx.createHttpServer();

    router.get("/api/healthcheck").handler(healthCheck);

    healthCheck.register("payment", 500, future -> {
      future.complete(Status.OK());
      future.complete(Status.KO());
    });

    httpServer.requestHandler(router::accept).listen(8081);
  }
}
