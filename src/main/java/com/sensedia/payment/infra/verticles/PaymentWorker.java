package com.sensedia.payment.infra.verticles;

import com.sensedia.payment.domain.data.Payment;
import com.sensedia.payment.infra.kafka.Consumer;
import com.sensedia.payment.infra.kafka.Producer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class PaymentWorker extends AbstractVerticle {


  public void start() {
    val consumer = Consumer
        .configureConsumer(this.vertx, "payment-group", config().getString("kafkaHost"),
            config().getInteger("kafkaPort").toString());

    consumer.handler(record -> {
      log.info("Processing key={}, value={}, partition={}, offset={}", record.key(),
          record.value(), record.partition(), record.offset());

      this.writeMessage(record.key(), record.value());

      consumer.commit(ar -> {
        if (ar.succeeded()) {
          log.info("Last read message offset committed");
        }
      });

    });

    consumer.subscribe("payment-topic", ar -> {
      if (ar.succeeded()) {
        log.info("payment-topic subscribed");
      } else {
        log.error("Could not subscribe payment-topic: {}", ar.cause().getMessage());
      }
    });
  }

  private void writeMessage(String id, JsonObject payment) {
    val config = new JsonObject()
        .put("host", config().getString("mongoHost"))
        .put("port", config().getInteger("mongoPort"))
        .put("db_name", "payment");

    val secureRandom = new SecureRandom();
    val paid = secureRandom.nextBoolean();
    val paymentObj = Json.decodeValue(payment.toString(), Payment.class);
    paymentObj.setPaid(paid);

    val client = MongoClient.createShared(vertx, config);
    client.save("payments", new JsonObject(Json.encode(paymentObj)), res -> {
      if (res.succeeded()) {

        log.info("Saved paynment with id " + id);

        val producer = Producer.configureProducer(this.vertx, config().getString("kafkaHost"),
            config().getInteger("kafkaPort").toString());
        val record =
            KafkaProducerRecord
                .create("payment-response-topic", id,
                    new JsonObject().put("paymentApproval", paid));

        producer.write(record, done -> {
          if (done.succeeded()) {
            log.info("Success send message to payment approval: {}", paid);
          } else {
            log.error("Error send message to payment approval: {}", done.cause().getMessage());
          }
        });
      } else {
        log.error("Error saving payment: {}, with cause: {}", payment,
            res.cause().getMessage());
      }
    });
  }
}
