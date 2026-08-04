package com.antra.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.antra.user.dto.LoginDTO;
import com.antra.user.dto.ProfileDTO;
import com.antra.user.dto.RegisterDTO;
import com.antra.user.dto.TokenDTO;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {
  @Test
  void registerReturnsCreatedOnSuccess() {
    UserService service = mock(UserService.class);
    when(service.register(any())).thenReturn(Optional.of(new TokenDTO("mocked-token")));
    AuthController controller = new AuthController(service);

    var response = controller.register(new RegisterDTO("alice", "alice@example.com", "password123"));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("mocked-token", response.getBody().token());
  }

  @Test
  void registerReturnsConflictWhenDuplicate() {
    UserService service = mock(UserService.class);
    when(service.register(any())).thenReturn(Optional.empty());
    AuthController controller = new AuthController(service);

    var response = controller.register(new RegisterDTO("alice", "alice@example.com", "password123"));
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void meReturnsUnauthorizedWhenUnauthenticated() {
    UserService service = mock(UserService.class);
    AuthController controller = new AuthController(service);

    assertThrows(ResponseStatusException.class, () -> controller.me(null));
  }

  @Test
  void meReturnsProfileWhenAuthenticated() {
    UserService service = mock(UserService.class);
    when(service.getProfileByUsername("alice")).thenReturn(Optional.of(new ProfileDTO(1L, "alice", "alice@example.com", "USER")));
    AuthController controller = new AuthController(service);

    var auth = new UsernamePasswordAuthenticationToken("alice", "pass");
    ProfileDTO profile = controller.me(auth);
    assertEquals("alice", profile.username());
    assertEquals("USER", profile.role());
  }
}
