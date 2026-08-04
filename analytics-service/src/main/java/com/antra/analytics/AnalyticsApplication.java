package com.antra.analytics;

import java.util.concurrent.atomic.LongAdder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
public class AnalyticsApplication {
  public static void main(String[] args) { SpringApplication.run(AnalyticsApplication.class, args); }

  @Component
  public static class Listener {
    private final LongAdder reservations = new LongAdder();
    private final ProcessedMessageRepository repo;

    public Listener(ProcessedMessageRepository repo) {
      this.repo = repo;
    }

    public long getReservationsCount() {
      return reservations.sum();
    }

    @Transactional
    @KafkaListener(topics="reservation-created", groupId="analytics")
    public void onReservation(ConsumerRecord<String, Object> event) {
      String key = event.topic() + ":" + event.partition() + ":" + event.offset();
      if (!repo.existsById(key)) {
        repo.save(new ProcessedMessage(key));
        reservations.increment();
        System.out.println("Reservations observed=" + reservations.sum());
      }
    }
  }
}
