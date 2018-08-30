package com.sensedia.payment.infra.verticles;

import io.vertx.core.AbstractVerticle;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentWorker extends AbstractVerticle {

  public void start() {
    this.vertx.eventBus().consumer("ecommerce.payment", handler -> {

      log.info("Received message message");

      handler.reply(handler.body());
      //handler.fail(1, "Call fail");
    });
  }
}
