package com.antra.user;

import com.antra.platform.security.JwtService;
import com.antra.user.dto.LoginDTO;
import com.antra.user.dto.ProfileDTO;
import com.antra.user.dto.RegisterDTO;
import com.antra.user.dto.TokenDTO;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository repository;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public UserService(UserRepository repository, JwtService jwtService) {
    this.repository = repository;
    this.jwtService = jwtService;
  }

  public Optional<TokenDTO> register(RegisterDTO r) {
    if (repository.existsByUsernameOrEmail(r.username(), r.email())) {
      return Optional.empty();
    }
    User user = repository.save(new User(r.username(), r.email(), encoder.encode(r.password())));
    String token = jwtService.issue(user.username, user.role);
    return Optional.of(new TokenDTO(token));
  }

  public Optional<TokenDTO> login(LoginDTO l) {
    return repository.findByUsername(l.username())
        .filter(u -> encoder.matches(l.password(), u.passwordHash))
        .map(u -> new TokenDTO(jwtService.issue(u.username, u.role)));
  }

  public Optional<ProfileDTO> getProfileByUsername(String username) {
    return repository.findByUsername(username)
        .map(u -> new ProfileDTO(u.id, u.username, u.email, u.role));
  }
}
