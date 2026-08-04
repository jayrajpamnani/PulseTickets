package com.antra.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.antra.user.dto.ProfileDTO;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class CurrentUserControllerTest {
  @Test
  void meReturnsUnauthorizedWithoutAuth() {
    UserService service = mock(UserService.class);
    CurrentUserController controller = new CurrentUserController(service);

    assertThrows(ResponseStatusException.class, () -> controller.me(null));
  }

  @Test
  void meReturnsUserProfileWhenAuthenticated() {
    UserService service = mock(UserService.class);
    when(service.getProfileByUsername("charlie")).thenReturn(Optional.of(new ProfileDTO(5L, "charlie", "charlie@example.com", "ADMIN")));
    CurrentUserController controller = new CurrentUserController(service);

    var auth = new UsernamePasswordAuthenticationToken("charlie", null);
    var profile = controller.me(auth);
    assertEquals("charlie", profile.username());
    assertEquals("ADMIN", profile.role());
  }
}
