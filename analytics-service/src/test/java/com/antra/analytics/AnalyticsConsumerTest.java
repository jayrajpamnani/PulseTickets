package com.antra.analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class AnalyticsConsumerTest {

  @Test
  void duplicateMessagesAreIgnored() {
    ProcessedMessageRepository repo = mock(ProcessedMessageRepository.class);
    AnalyticsApplication.Listener listener = new AnalyticsApplication.Listener(repo);

    ConsumerRecord<String, Object> record = new ConsumerRecord<>("reservation-created", 0, 42L, "key", "payload");
    String expectedKey = "reservation-created:0:42";

    when(repo.existsById(expectedKey)).thenReturn(false).thenReturn(true);

    // First invocation increments count
    listener.onReservation(record);
    assertEquals(1, listener.getReservationsCount());
    verify(repo, times(1)).save(any(ProcessedMessage.class));

    // Duplicate message ignored
    listener.onReservation(record);
    assertEquals(1, listener.getReservationsCount());
    verify(repo, times(1)).save(any(ProcessedMessage.class));
  }
}
