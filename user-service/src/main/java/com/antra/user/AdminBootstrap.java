package com.antra.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
class AdminBootstrap {
  @Bean CommandLineRunner bootstrapAdmin(UserRepository users,@Value("${app.admin.username:}") String username,@Value("${app.admin.email:}") String email,@Value("${app.admin.password:}") String password){
    return args -> { if(!username.isBlank()&&!password.isBlank()&&users.findByUsername(username).isEmpty()){User admin=new User(username,email.isBlank()?username+"@example.com":email,new BCryptPasswordEncoder().encode(password));admin.role="ADMIN";users.save(admin);} };
  }
}
