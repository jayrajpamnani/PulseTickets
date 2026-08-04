package com.antra.analytics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class AnalyticsApplication {
  public static void main(String[] args) { SpringApplication.run(AnalyticsApplication.class, args); }

  @Component
  static class Listener {
    private final LongAdder reservations = new LongAdder();
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics="reservation-created", groupId="analytics")
    void onReservation(ConsumerRecord<String, Object> event) {
      if (processed.add(event.topic() + ":" + event.partition() + ":" + event.offset())) {
        reservations.increment();
        System.out.println("Reservations observed=" + reservations.sum());
      }
    }
  }
}
