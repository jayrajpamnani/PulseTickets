package com.antra.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {
  @Id
  private String messageKey;

  @Column(nullable = false)
  private Instant processedAt;

  protected ProcessedMessage() {}

  public ProcessedMessage(String messageKey) {
    this.messageKey = messageKey;
    this.processedAt = Instant.now();
  }

  public String getMessageKey() { return messageKey; }
  public Instant getProcessedAt() { return processedAt; }
}
