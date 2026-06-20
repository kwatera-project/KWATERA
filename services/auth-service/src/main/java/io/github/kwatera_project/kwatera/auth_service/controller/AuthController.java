package io.github.kwatera_project.kwatera.auth_service.controller;

import io.github.kwatera_project.kwatera.auth_service.dto.AuthResponse;
import io.github.kwatera_project.kwatera.auth_service.dto.LoginRequest;
import io.github.kwatera_project.kwatera.auth_service.dto.RegisterRequest;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${kwatera.urls.frontend-base}")
public class AuthController {

  private final UserService userService;

  private final AuthenticationManager authenticationManager;

  private final JwtService jwtService;

  private final UserRepository userRepository;

  @PostMapping("/register")
  public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(
        request.getUsername(),
        request.getEmail(),
        request.getRole(),
        request.getPassword(),
        request.getFirstName(),
        request.getLastName());
    return ResponseEntity.ok("User registered");
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

    User userEntity =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("userId", userEntity.getId().toString());
    extraClaims.put("firstName", userEntity.getFirstName());
    extraClaims.put("lastName", userEntity.getLastName());

    String token = jwtService.generateToken(extraClaims, userDetails);

    return ResponseEntity.ok(new AuthResponse(token));
  }
}
