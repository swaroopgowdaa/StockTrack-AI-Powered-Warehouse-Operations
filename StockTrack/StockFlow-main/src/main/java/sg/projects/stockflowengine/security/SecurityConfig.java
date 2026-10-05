package sg.projects.stockflowengine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import sg.projects.stockflowengine.service.AppUserService;

/**
 * Central Spring Security configuration defining authentication and
 * authorization rules for the entire application.
 *
 * <p>Key security decisions:
 * <ul>
 *   <li>CSRF disabled — not needed for stateless REST APIs</li>
 *   <li>STATELESS session — no server-side sessions, JWT handles state</li>
 *   <li>JWT filter runs before Spring's default auth filter on every request</li>
 *   <li>BCrypt password verification via {@link DaoAuthenticationProvider}</li>
 * </ul>
 *
 * <p>Role-based access control matrix:
 * <pre>
 * /api/auth/**                    → PUBLIC (no token required)
 * /api/users/**                   → ADMIN only
 * PUT /api/inwards/{id}/status    → MANAGER, ADMIN
 * PUT /api/outwards/{id}/complete → MANAGER, ADMIN
 * Everything else                 → Any authenticated user
 * </pre>
 *
 * <p>{@link PasswordEncoder} is injected from {@link PasswordConfig} rather
 * than defined here to avoid a circular dependency between this class,
 * {@link JwtAuthFilter} and {@link AppUserService}.
 *
 * @see JwtAuthFilter
 * @see PasswordConfig
 * @see JwtService
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs SecurityConfig with required security dependencies.
     *
     * @param jwtAuthFilter   JWT filter for token validation on every request
     * @param appUserService  user details service for loading users from database
     * @param passwordEncoder BCrypt encoder injected from {@link PasswordConfig}
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AppUserService appUserService,
                          PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Configures the security filter chain with authorization rules and JWT integration.
     *
     * <p>Session management is set to STATELESS — no {@link jakarta.servlet.http.HttpSession}
     * is created, allowing the application to scale horizontally without session sharing.
     *
     * @param http the HttpSecurity builder to configure
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF
                .csrf(csrf -> csrf.disable())

                // Define access rules per endpoint
                .authorizeHttpRequests(auth -> auth

                        // Public endpoint — login excluded from token requirement
                        .requestMatchers("/api/auth/**").permitAll()

                        // ADMIN only — user management
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // MANAGER + ADMIN — approve/reject inwards
                        .requestMatchers(HttpMethod.PUT, "/api/inwards/*/status")
                        .hasAnyRole("MANAGER", "ADMIN")

                        // MANAGER + ADMIN — complete outwards
                        .requestMatchers(HttpMethod.PUT, "/api/outwards/*/complete")
                        .hasAnyRole("MANAGER", "ADMIN")

                        // Everyone authenticated can access everything else
                        .anyRequest().authenticated()
                )

                // STATELESS — no server-side sessions
                // Every request must carry its own valid Bearer token
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Register our custom authentication provider
                .authenticationProvider(authenticationProvider())

                // This JWT filter runs BEFORE Spring's default username/password filter
                // Ensures token validation happens first on every request
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures the authentication provider with user details service and password encoder.
     *
     * <p>{@link DaoAuthenticationProvider} bridges Spring Security with the database:
     * <ol>
     *   <li>Calls {@link AppUserService#loadUserByUsername} to fetch user from DB</li>
     *   <li>Uses {@link PasswordEncoder} to verify BCrypt hashed password</li>
     *   <li>Checks {@link sg.projects.stockflowengine.model.AppUser#isEnabled()} for ACTIVE status</li>
     * </ol>
     *
     * @return configured DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // UserDetailsService passed via constructor
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(appUserService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Exposes the AuthenticationManager bean for use in {@link sg.projects.stockflowengine.controller.AuthController}.
     *
     * <p>Spring automatically builds the AuthenticationManager from the
     * registered {@link AuthenticationProvider} — no manual wiring needed.
     *
     * @param config Spring's authentication configuration
     * @return the configured AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
