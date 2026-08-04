package com.antra.event;

import com.antra.event.dto.CreateEventDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EventBootstrap {
  @Bean CommandLineRunner seedEvents(EventRepository repository) {
    return args -> {
      if (repository.count() > 0) return;
      Instant now = Instant.now();
      repository.save(event("Neon Nights: Rooftop Sessions", "Skyline Terrace, New York", 5, "48.00", 180, "Live DJs, city lights, and sunset cocktails.", "https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=1000&q=85", now));
      repository.save(event("The Immersive Art House", "Brooklyn Navy Yard, New York", 9, "32.50", 120, "A late-night gallery experience with projection art and sound.", "https://images.unsplash.com/photo-1549490349-8643362247b5?auto=format&fit=crop&w=1000&q=85", now));
      repository.save(event("Jazz Under the Stars", "The Garden Stage, Chicago", 14, "65.00", 250, "An open-air evening of modern jazz and classic cocktails.", "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=1000&q=85", now));
      repository.save(event("Sunday Makers Market", "Harbor Hall, San Francisco", 18, "12.00", 500, "Local makers, small-batch food, and live acoustic sets.", "https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1000&q=85", now));
    };
  }

  private Event event(String title, String venue, int days, String price, int capacity, String description, String banner, Instant now) {
    return new Event(new CreateEventDTO(title, venue, now.plus(days, ChronoUnit.DAYS), new BigDecimal(price), capacity, description, banner));
  }
}
