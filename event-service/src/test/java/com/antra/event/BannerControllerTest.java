package com.antra.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class BannerControllerTest {
  @Test
  void unconfiguredBucketThrowsServiceUnavailable() {
    EventRepository repo = mock(EventRepository.class);
    BannerController controller = new BannerController(repo, "", "us-east-1", "");

    var ex = assertThrows(ResponseStatusException.class, () -> controller.createUpload(1L, "image/png"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
  }

  @Test
  void attachMissingEventThrowsNotFound() {
    EventRepository repo = mock(EventRepository.class);
    when(repo.findById(99L)).thenReturn(Optional.empty());
    BannerController controller = new BannerController(repo, "bucket", "us-east-1", "");

    var body = new BannerController.BannerUpdate("https://example.com/banner.jpg");
    var ex = assertThrows(ResponseStatusException.class, () -> controller.attach(99L, body));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }
}
