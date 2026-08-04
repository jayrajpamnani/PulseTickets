package com.antra.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableMethodSecurity
public class ApiSecurity {
  @Bean SecurityFilterChain security(HttpSecurity http, JwtService jwt) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/{id}").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
