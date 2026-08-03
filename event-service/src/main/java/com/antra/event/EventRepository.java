package com.antra.event; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*;
interface EventRepository extends JpaRepository<Event,Long> { Page<Event> findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCase(String title, String venue, Pageable page); }
