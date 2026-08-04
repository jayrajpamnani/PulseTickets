package com.antra.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

class NotificationConsumerTest {

  @Test
  void duplicateMessagesAreIgnored() {
    ProcessedMessageRepository repo = mock(ProcessedMessageRepository.class);
    NotificationApplication.Listener listener = new NotificationApplication.Listener(repo);

    ConsumerRecord<String, Object> record = new ConsumerRecord<>("reservation-created", 0, 100L, "key", "payload");
    String expectedKey = "reservation:reservation-created:0:100";

    when(repo.existsById(expectedKey)).thenReturn(false).thenReturn(true);

    // First invocation processes and saves message
    listener.reservation(record);
    verify(repo, times(1)).save(any(ProcessedMessage.class));

    // Second invocation sees duplicate and ignores
    listener.reservation(record);
    verify(repo, times(1)).save(any(ProcessedMessage.class));
  }

  @Test
  void errorHandlerConfiguredWithDLQ() {
    KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);
    NotificationApplication app = new NotificationApplication();
    DefaultErrorHandler handler = app.kafkaErrorHandler(template);
    assertNotNull(handler);
  }
}
