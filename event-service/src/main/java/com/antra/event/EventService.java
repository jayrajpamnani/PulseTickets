package com.antra.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventService {
  private final EventRepository repository;
  EventService(EventRepository repository) { this.repository = repository; }
  Page<Event> search(String keyword, Pageable pageable) { return repository.findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, keyword, pageable); }
  Event find(Long id) { return repository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)); }
  Event save(Event event) { return repository.save(event); }
  boolean exists(Long id) { return repository.existsById(id); }
  void delete(Long id) { repository.deleteById(id); }
}
