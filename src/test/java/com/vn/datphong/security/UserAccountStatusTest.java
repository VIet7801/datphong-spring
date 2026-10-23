package com.vn.datphong.security;

import com.vn.datphong.config.SecurityConfig;
import com.vn.datphong.controller.AuthenticationController;
import com.vn.datphong.entity.Role;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.exception.GlobalExceptionHandler;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.repository.RolePermissionRepository;
import com.vn.datphong.service.AuthenticationService;
import com.vn.datphong.service.identity.AccountSessionService;
import com.vn.datphong.service.identity.AccountRegistrationService;
import com.vn.datphong.dto.response.AuthTokenResponse;
import com.vn.datphong.service.Impl.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitConfig(UserAccountStatusTest.TestConfig.class)
@WebAppConfiguration
class UserAccountStatusTest {
    private static final String LOGIN = """
            {"username":"alice","password":"test-password"}
            """;

    @TestConfiguration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, CustomUserDetailsService.class, AuthenticationService.class,
            AuthenticationController.class, GlobalExceptionHandler.class, ProtectedEndpoint.class})
    static class TestConfig {
        @Bean
        AuthRateLimiter limiter() { return mock(AuthRateLimiter.class); }

        @Bean
        AccountRegistrationService registration() { return mock(AccountRegistrationService.class); }

        @Bean
        AccountSessionService sessions(JwtTokenProvider tokens) {
            AccountSessionService service = mock(AccountSessionService.class);
            when(service.open(any(AccountPrincipal.class))).thenAnswer(call -> {
                AccountPrincipal principal = call.getArgument(0);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                return new AuthTokenResponse(tokens.generateToken(auth), "test-refresh", "Bearer", 60, 600,
                        new AuthTokenResponse.UserSummary(principal.getUserId(), "Alice", principal.getUsername(),
                                Set.of("ROLE_USER"), Set.of()));
            });
            return service;
        }
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        RolePermissionRepository rolePermissionRepository() {
            return mock(RolePermissionRepository.class);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            return new JwtTokenProvider(Base64.getEncoder().encodeToString(key), 60_000L);
        }
    }

    @RestController
    @TestComponent
    static class ProtectedEndpoint {
        @GetMapping("/account-status-test")
        Map<String, Boolean> protectedResource() {
            return Map.of("success", true);
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository repository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private org.springframework.security.core.userdetails.UserDetailsService detailsService;
    @Autowired private JwtTokenProvider tokens;
    private final AtomicReference<User> currentUser = new AtomicReference<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(repository);
        currentUser.set(User.builder().id(1L).username("alice")
                .password(encoder.encode("test-password"))
                .active(true).status(UserStatus.ACTIVE).emailVerifiedAt(java.time.Instant.now())
                .roles(Set.of(Role.builder().code("ROLE_USER").build())).build());
        when(repository.findByUsername("alice"))
                .thenAnswer(invocation -> Optional.ofNullable(currentUser.get()));
        when(repository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(currentUser.get()));
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String tokenForActiveUser() {
        var details = detailsService.loadUserByUsername("alice");
        return tokens.generateToken(new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    static Stream<Arguments> deniedStates() {
        return Stream.of(
                Arguments.of(UserStatus.IN_ACTIVE, true),
                Arguments.of(UserStatus.PENDING_VERIFICATION, true),
                Arguments.of(UserStatus.LOCKED, true),
                Arguments.of(UserStatus.ACTIVE, false),
                Arguments.of(UserStatus.ACTIVE, null),
                Arguments.of(null, true));
    }

    @Test
    void activeAccountCanLoginAndUseSignedToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        mvc.perform(get("/account-status-test").header("Authorization", "Bearer " + tokenForActiveUser()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("deniedStates")
    void disabledOrLockedAccountCannotLogin(UserStatus status, Boolean active) throws Exception {
        currentUser.get().setStatus(status);
        currentUser.get().setActive(active);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @ParameterizedTest
    @MethodSource("deniedStates")
    void statusChangeRejectsPreviouslyIssuedToken(UserStatus status, Boolean active) throws Exception {
        String token = tokenForActiveUser();
        currentUser.get().setStatus(status);
        currentUser.get().setActive(active);
        assertTrue(tokens.isTokenValid(token)); // Token còn hạn và có chữ ký hợp lệ.
        mvc.perform(get("/account-status-test").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockedAdminCannotUsePreviouslyIssuedToken() throws Exception {
        currentUser.get().setRoles(Set.of(Role.builder().code("ROLE_ADMIN").build()));
        String token = tokenForActiveUser();
        currentUser.get().setStatus(UserStatus.LOCKED);
        assertFalse(detailsService.loadUserByUsername("alice").isAccountNonLocked());
        mvc.perform(get("/account-status-test").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incrementedTokenVersionRejectsOldTokenEvenAfterAccountIsUnlocked() throws Exception {
        String oldToken = tokenForActiveUser();
        currentUser.get().setTokenVersion(1L);
        mvc.perform(get("/account-status-test").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/account-status-test").header("Authorization", "Bearer " + tokenForActiveUser()))
                .andExpect(status().isOk());
    }

    @Test
    void deletedAccountCannotLoginOrUseOldToken() throws Exception {
        String token = tokenForActiveUser();
        currentUser.set(null);
        assertThrows(UsernameNotFoundException.class, () -> detailsService.loadUserByUsername("alice"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/account-status-test").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enumContainsTheFourCatalogStatuses() {
        assertArrayEquals(new UserStatus[]{UserStatus.ACTIVE, UserStatus.IN_ACTIVE, UserStatus.LOCKED, UserStatus.PENDING_VERIFICATION},
                UserStatus.values());
    }
}
