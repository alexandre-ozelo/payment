package com.sensedia.payment;

import com.sensedia.payment.infra.verticles.PaymentWorker;
import com.sensedia.payment.domain.resource.Payment;
import io.netty.util.internal.StringUtil;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class PaymentApplication {

  public static void main(String[] args) {
    val vertx = Vertx.vertx();

    val envStore = new ConfigStoreOptions().setType("env");
    val options = new ConfigRetrieverOptions().addStore(envStore);
    val retriever = ConfigRetriever.create(vertx, options);

    log.info("Starting application...");

    retriever.getConfig(conf -> {
      if (conf.succeeded()) {
        val config = conf.result();

        val metadata = new JsonObject()
            .put("mongoHost", StringUtil.isNullOrEmpty(config.getString("MONGO_HOST")) ? "localhost"
                : config.getString("MONGO_HOST"))
            .put("mongoPort", Objects.isNull(config.getInteger("MONGO_PORT")) ? 27017
                : config.getInteger("MONGO_PORT"))
            .put("kafkaHost", StringUtil.isNullOrEmpty(config.getString("KAFKA_HOST")) ? "localhost"
                : config.getString("KAFKA_HOST"))
            .put("kafkaPort", Objects.isNull(config.getInteger("KAFKA_PORT")) ? 9092
                : config.getInteger("KAFKA_PORT"));

        log.info("Enviroment data: {}", metadata.toString());

        val deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(metadata);

        vertx.deployVerticle(new Payment(), deploymentOptions.setWorker(false));
        vertx.deployVerticle(new PaymentWorker(), deploymentOptions.setWorker(true));
      }
    });
  }
}
