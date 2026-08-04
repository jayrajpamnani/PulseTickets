package com.antra.notification;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class NotificationApplication {
  public static void main(String[] args) { SpringApplication.run(NotificationApplication.class, args); }

  @Bean
  DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
    return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 2L));
  }

  @Component
  static class Listener {
    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics="reservation-created", groupId="notification")
    void reservation(ConsumerRecord<String, Object> event) {
      if (processed.add("reservation:" + event.topic() + ":" + event.partition() + ":" + event.offset()))
        System.out.println("Confirmation queued: " + event.value());
    }

    @KafkaListener(topics="payment-completed", groupId="notification")
    void payment(ConsumerRecord<String, Object> event) {
      if (processed.add("payment:" + event.topic() + ":" + event.partition() + ":" + event.offset()))
        System.out.println("Payment receipt queued: " + event.value());
    }
  }
}
