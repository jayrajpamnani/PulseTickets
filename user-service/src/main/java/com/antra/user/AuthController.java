package com.antra.user;

import com.antra.user.dto.LoginDTO;
import com.antra.user.dto.ProfileDTO;
import com.antra.user.dto.RegisterDTO;
import com.antra.user.dto.TokenDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ResponseEntity<TokenDTO> register(@Valid @RequestBody RegisterDTO r) {
    return userService.register(r)
        .map(token -> ResponseEntity.status(HttpStatus.CREATED).body(token))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
  }

  @PostMapping("/login")
  public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO l) {
    return userService.login(l)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @GetMapping("/me")
  public ProfileDTO me(Authentication a) {
    if (a == null || a.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return userService.getProfileByUsername(a.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }
}
