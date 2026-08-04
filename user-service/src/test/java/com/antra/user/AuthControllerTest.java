package com.antra.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import com.antra.platform.security.JwtService;

class AuthControllerTest {
  @Test void registrationStoresAHashedPassword() {
    UserRepository repository = mock(UserRepository.class);
    when(repository.existsByUsernameOrEmail("alice", "alice@example.com")).thenReturn(false);
    when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    AuthController controller = new AuthController(repository, new JwtService("12345678901234567890123456789012"));
    var response = controller.register(new AuthController.Register("alice", "alice@example.com", "password123"));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(repository).save(argThat(user -> user.passwordHash != null && !user.passwordHash.equals("password123")));
  }
}
