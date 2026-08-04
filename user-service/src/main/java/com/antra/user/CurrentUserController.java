package com.antra.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class CurrentUserController {
  private final UserRepository users;
  CurrentUserController(UserRepository users) { this.users = users; }

  @GetMapping("/me")
  Profile me(Authentication authentication) {
    User user = users.findByUsername(authentication.getName()).orElseThrow();
    return new Profile(user.id, user.username, user.email, user.role);
  }

  record Profile(Long id, String username, String email, String role) {}
}
