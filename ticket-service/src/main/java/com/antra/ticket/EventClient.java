package com.antra.ticket;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventClient {
  private final RestClient client;
  private final AtomicInteger failures = new AtomicInteger();
  private volatile Instant openUntil = Instant.MIN;

  EventClient(@Value("${services.event.url:http://event-service:8082}") String url) {
    var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    factory.setReadTimeout(Duration.ofSeconds(4));
    client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  Map<?, ?> reserve(Long eventId, int quantity, String bearer) {
    return execute(() -> client.post().uri("/api/events/{id}/reserve?quantity={q}", eventId, quantity)
        .header("Authorization", bearer).retrieve().body(Map.class));
  }

  void release(Long eventId, int quantity, String bearer) {
    execute(() -> { client.post().uri("/api/events/{id}/release?quantity={q}", eventId, quantity)
        .header("Authorization", bearer).retrieve().toBodilessEntity(); return Map.of(); });
  }

  private <T> T execute(java.util.function.Supplier<T> operation) {
    if (Instant.now().isBefore(openUntil)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event service temporarily unavailable");
    RestClientException last = null;
    for (int attempt = 1; attempt <= 3; attempt++) {
      try { T result = operation.get(); failures.set(0); return result; }
      catch (HttpStatusCodeException ex) {
        throw new ResponseStatusException(ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
      }
      catch (RestClientException ex) { last = ex; if (attempt < 3) try { Thread.sleep(100L * attempt); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; } }
    }
    if (failures.incrementAndGet() >= 5) openUntil = Instant.now().plusSeconds(30);
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event service unavailable", last);
  }
}
