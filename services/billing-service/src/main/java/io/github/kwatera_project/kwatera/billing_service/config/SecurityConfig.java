package io.github.kwatera_project.kwatera.billing_service.config;

import io.github.kwatera_project.kwatera.billing_service.filter.JwtAuthFilter;
import java.util.List;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring bean injected via constructor")
  public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.csrf(
            csrf ->
                csrf.disable()) // NOSONAR: CSRF is disabled because the API is stateless and uses
        // JWT tokens
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/billing/webhook")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/dashboard/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_OWNER")
                    .requestMatchers("/api/billing/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
