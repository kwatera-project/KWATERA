package io.github.kwatera_project.kwatera.review_service.controller;

import io.github.kwatera_project.kwatera.review_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.review_service.service.JwtService;
import io.github.kwatera_project.kwatera.review_service.service.ReviewService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AdminReviewController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "kwatera.security.internal-token=test-internal-token")
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtService jwtService;

    private UsernamePasswordAuthenticationToken adminAuth;
    private UsernamePasswordAuthenticationToken guestAuth;

    @BeforeEach
    void setUp() {
        adminAuth = auth("ROLE_ADMIN");
        guestAuth = auth("ROLE_GUEST");
    }

    @Test
    void shouldReturnReviewsForAdmin() throws Exception {
        when(reviewService.getReviewsForAdmin(null, null, "createdAt"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/admin/reviews")
                                .with(authentication(adminAuth))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(reviewService)
                .getReviewsForAdmin(null, null, "createdAt");
    }

    @Test
    void shouldReturnForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reviews")
                                .with(authentication(guestAuth))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(reviewService);
    }

    @Test
    void shouldReturnUnauthorizedForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }

    @Test
    void shouldFilterReviewsByDate() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(reviewService.getReviewsForAdmin(startDate, endDate, "createdAt"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/admin/reviews")
                                .with(authentication(adminAuth))
                                .param("startDate", "2026-01-01")
                                .param("endDate", "2026-01-31")
                                .param("dateType", "createdAt")
                )
                .andExpect(status().isOk());

        verify(reviewService)
                .getReviewsForAdmin(startDate, endDate, "createdAt");
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                "test-user",
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}