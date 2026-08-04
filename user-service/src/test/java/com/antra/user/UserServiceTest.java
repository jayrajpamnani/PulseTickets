package com.antra.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.antra.platform.security.JwtService;
import com.antra.user.dto.LoginDTO;
import com.antra.user.dto.RegisterDTO;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {
  private UserRepository repository;
  private JwtService jwtService;
  private UserService userService;

  @BeforeEach
  void setUp() {
    repository = mock(UserRepository.class);
    jwtService = new JwtService("12345678901234567890123456789012");
    userService = new UserService(repository, jwtService);
  }

  @Test
  void registerSuccess() {
    when(repository.existsByUsernameOrEmail("alice", "alice@example.com")).thenReturn(false);
    when(repository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    var result = userService.register(new RegisterDTO("alice", "alice@example.com", "password123"));
    assertTrue(result.isPresent());
    assertNotNull(result.get().token());
  }

  @Test
  void registerDuplicateFails() {
    when(repository.existsByUsernameOrEmail("alice", "alice@example.com")).thenReturn(true);

    var result = userService.register(new RegisterDTO("alice", "alice@example.com", "password123"));
    assertTrue(result.isEmpty());
  }

  @Test
  void loginSuccessAndFailure() {
    org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    User u = new User("bob", "bob@example.com", encoder.encode("secret123"));
    when(repository.findByUsername("bob")).thenReturn(Optional.of(u));

    var success = userService.login(new LoginDTO("bob", "secret123"));
    assertTrue(success.isPresent());

    var fail = userService.login(new LoginDTO("bob", "wrongpass"));
    assertTrue(fail.isEmpty());
  }
}
