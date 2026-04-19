package io.github.kwatera_project.kwatera.auth_service.controller;

import io.github.kwatera_project.kwatera.auth_service.dto.AuthResponse;
import io.github.kwatera_project.kwatera.auth_service.dto.LoginRequest;
import io.github.kwatera_project.kwatera.auth_service.dto.RegisterRequest;
import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

  private final UserService userService;

  private final AuthenticationManager authenticationManager;

  private final JwtService jwtService;

  @PostMapping("/register")
  public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(
        request.getUsername(), request.getEmail(), request.getRole(), request.getPassword());
    return ResponseEntity.ok("User registered");
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

      UserDetails user = (UserDetails) authentication.getPrincipal();

      String token = jwtService.generateToken(user);

      return ResponseEntity.ok(new AuthResponse(token));

    } catch (AuthenticationException e) {
      return ResponseEntity.status(401).body("Invalid credentials");
    }
  }
}
