package io.github.kwatera_project.kwatera.billing_service.controller;

import static io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType.ACCOMMODATION;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.billing_service.dto.CheckoutRequest;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.service.PaymentService;
import io.github.kwatera_project.kwatera.billing_service.service.StripeService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BillingControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private BillingController billingController;

  @MockitoBean private PaymentService paymentService;

  @MockitoBean private StripeService stripeService;

  @Autowired private ObjectMapper objectMapper;

  private static final String TEST_SECRET =
      "my_super_secret_key_which_is_long_enough_for_hmac_sha256";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(billingController, "secret", TEST_SECRET);
    SecurityContextHolder.clearContext();
  }

  private String createToken(String subject, String userId, List<String> roles) {
    Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .setSubject(subject)
        .claim("userId", userId)
        .claim("role", roles)
        .signWith(key)
        .compact();
  }

  @Test
  void shouldCreateCheckout() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = createToken("guest", guestId.toString(), List.of("ROLE_USER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    CheckoutRequest request = new CheckoutRequest();
    request.setType(ACCOMMODATION);
    request.setDescription("test");
    request.setQuantity(new BigDecimal(2));
    request.setUnitPrice(new BigDecimal(100));

    when(paymentService.createCheckoutSession(
            eq(reservationId),
            eq("Bearer " + token),
            eq(ACCOMMODATION),
            eq("test"),
            any(BigDecimal.class),
            any(BigDecimal.class)))
        .thenReturn("https://stripe.checkout/session");

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            post("/api/billing/checkout/" + reservationId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("https://stripe.checkout/session"));
  }

  @Test
  void shouldReturnSettlementWhenOwner() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = createToken("guest", guestId.toString(), List.of("ROLE_USER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    SettlementResponseDto dto = mock(SettlementResponseDto.class);

    when(paymentService.getSettlementWithItems(reservationId, "Bearer " + token)).thenReturn(dto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnForbiddenWhenNotOwner() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID otherGuestId = UUID.randomUUID();

    String token = createToken("guest", otherGuestId.toString(), List.of("ROLE_USER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnSettlementWhenAdmin() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = createToken("admin", UUID.randomUUID().toString(), List.of("ROLE_ADMIN"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    SettlementResponseDto dto = mock(SettlementResponseDto.class);

    when(paymentService.getSettlementWithItems(reservationId, "Bearer " + token)).thenReturn(dto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnSettlementWhenOwnerRoleOwner() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = createToken("owner", UUID.randomUUID().toString(), List.of("ROLE_OWNER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    SettlementResponseDto dto = mock(SettlementResponseDto.class);

    when(paymentService.getSettlementWithItems(reservationId, "Bearer " + token)).thenReturn(dto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "owner", null, List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnSettlementItemInfoWhenOwner() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = createToken("guest", guestId.toString(), List.of("ROLE_USER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    io.github.kwatera_project.kwatera.billing_service.dto.SettlementItemDto dto =
        mock(io.github.kwatera_project.kwatera.billing_service.dto.SettlementItemDto.class);

    when(paymentService.getSettlementItemInfoByType(
            reservationId,
            io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType
                .ACCOMMODATION,
            "Bearer " + token))
        .thenReturn(dto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId + "/ACCOMMODATION")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnNotFoundWhenReservationNull() throws Exception {
    UUID reservationId = UUID.randomUUID();

    String token = createToken("guest", UUID.randomUUID().toString(), List.of("ROLE_USER"));

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(null);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnForbiddenWhenNoAuthentication() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = createToken("guest", guestId.toString(), List.of("ROLE_USER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    SecurityContextHolder.clearContext();

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbiddenWhenReservationUserIdNull() throws Exception {
    UUID reservationId = UUID.randomUUID();

    String token = createToken("guest", UUID.randomUUID().toString(), List.of("ROLE_USER"));

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(null);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbiddenWhenAuthenticationDetailsNull() throws Exception {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    String token = "invalid-token-signature";

    ReservationDto reservationDto = new ReservationDto();
    reservationDto.setId(reservationId);
    reservationDto.setUserId(guestId);

    when(stripeService.getReservation(reservationId, "Bearer " + token)).thenReturn(reservationDto);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "guest", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
