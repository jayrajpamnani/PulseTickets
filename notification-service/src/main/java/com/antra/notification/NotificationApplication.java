package com.antra.notification;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.backoff.FixedBackOff;

@SpringBootApplication
public class NotificationApplication {
  public static void main(String[] args) { SpringApplication.run(NotificationApplication.class, args); }

  @Bean
  DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
    return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 2L));
  }

  @Component
  public static class Listener {
    private final ProcessedMessageRepository repo;

    public Listener(ProcessedMessageRepository repo) {
      this.repo = repo;
    }

    @Transactional
    @KafkaListener(topics="reservation-created", groupId="notification")
    public void reservation(ConsumerRecord<String, Object> event) {
      String key = "reservation:" + event.topic() + ":" + event.partition() + ":" + event.offset();
      if (!repo.existsById(key)) {
        repo.save(new ProcessedMessage(key));
        System.out.println("Confirmation queued: " + event.value());
      }
    }

    @Transactional
    @KafkaListener(topics="payment-completed", groupId="notification")
    public void payment(ConsumerRecord<String, Object> event) {
      String key = "payment:" + event.topic() + ":" + event.partition() + ":" + event.offset();
      if (!repo.existsById(key)) {
        repo.save(new ProcessedMessage(key));
        System.out.println("Payment receipt queued: " + event.value());
      }
    }
  }
}
