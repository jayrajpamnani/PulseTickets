package com.antra.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserRepositoryTest {
  @Test
  void testFindByUsernameAndExists() {
    UserRepository repo = mock(UserRepository.class);
    User user = new User("alice", "alice@example.com", "hash123");
    when(repo.findByUsername("alice")).thenReturn(Optional.of(user));
    when(repo.existsByUsernameOrEmail("alice", "alice@example.com")).thenReturn(true);

    assertTrue(repo.findByUsername("alice").isPresent());
    assertEquals("alice@example.com", repo.findByUsername("alice").get().email);
    assertTrue(repo.existsByUsernameOrEmail("alice", "alice@example.com"));
    assertFalse(repo.existsByUsernameOrEmail("bob", "bob@example.com"));
  }
}
