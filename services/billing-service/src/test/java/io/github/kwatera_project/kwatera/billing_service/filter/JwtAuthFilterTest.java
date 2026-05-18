package io.github.kwatera_project.kwatera.billing_service.filter;

import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @InjectMocks private JwtAuthFilter filter;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(filter, "secret", "test_secret");
  }

  @Test
  void shouldSkipWhenNoAuthorizationHeader() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(response);
  }

  @Test
  void shouldSkipWhenHeaderDoesNotStartWithBearer() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic abc");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldReturnUnauthorizedOnInvalidToken() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verifyNoInteractions(filterChain);
  }
}
